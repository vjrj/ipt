/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gbif.ipt.service.manage.impl;

import org.gbif.common.parsers.core.OccurrenceParseResult;
import org.gbif.common.parsers.core.ParseResult;
import org.gbif.common.parsers.date.DateParsers;
import org.gbif.common.parsers.date.TemporalParser;
import org.gbif.common.parsers.geospatial.CoordinateParseUtils;
import org.gbif.common.parsers.geospatial.LatLng;
import org.gbif.ipt.action.portal.OrganizedTaxonomicCoverage;
import org.gbif.ipt.action.portal.OrganizedTaxonomicKeywords;
import org.gbif.ipt.config.Constants;
import org.gbif.ipt.model.DataSchemaFieldMapping;
import org.gbif.ipt.model.DataSchemaMapping;
import org.gbif.ipt.model.ExtensionMapping;
import org.gbif.ipt.model.InferredCamtrapGeographicScope;
import org.gbif.ipt.model.InferredCamtrapMetadata;
import org.gbif.ipt.model.InferredCamtrapTaxonomicScope;
import org.gbif.ipt.model.InferredCamtrapTemporalScope;
import org.gbif.ipt.model.InferredEmlGeographicCoverage;
import org.gbif.ipt.model.InferredEmlMetadata;
import org.gbif.ipt.model.InferredEmlTaxonomicCoverage;
import org.gbif.ipt.model.InferredEmlTemporalCoverage;
import org.gbif.ipt.model.InferredMetadata;
import org.gbif.ipt.model.PropertyMapping;
import org.gbif.ipt.model.Resource;
import org.gbif.ipt.model.datapackage.metadata.camtrap.Taxonomic;
import org.gbif.ipt.service.admin.VocabulariesManager;
import org.gbif.ipt.service.manage.ResourceMetadataInferringService;
import org.gbif.ipt.service.manage.SourceManager;
import org.gbif.metadata.eml.ipt.model.BBox;
import org.gbif.metadata.eml.ipt.model.GeospatialCoverage;
import org.gbif.metadata.eml.ipt.model.Point;
import org.gbif.metadata.eml.ipt.model.TaxonKeyword;
import org.gbif.metadata.eml.ipt.model.TaxonomicCoverage;
import org.gbif.metadata.eml.ipt.model.TemporalCoverage;
import org.gbif.metadata.eml.ipt.util.DateUtils;
import org.gbif.utils.file.ClosableReportingIterator;

import java.text.ParseException;
import java.time.YearMonth;
import java.time.chrono.ChronoLocalDate;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import static org.gbif.ipt.config.Constants.CAMTRAP_DP;
import static org.gbif.ipt.config.Constants.CLASS;
import static org.gbif.ipt.config.Constants.FAMILY;
import static org.gbif.ipt.config.Constants.KINGDOM;
import static org.gbif.ipt.config.Constants.ORDER;
import static org.gbif.ipt.config.Constants.PHYLUM;
import static org.gbif.ipt.config.Constants.VOCAB_CLASS;
import static org.gbif.ipt.config.Constants.VOCAB_DECIMAL_LATITUDE;
import static org.gbif.ipt.config.Constants.VOCAB_DECIMAL_LONGITUDE;
import static org.gbif.ipt.config.Constants.VOCAB_EVENT_DATE;
import static org.gbif.ipt.config.Constants.VOCAB_FAMILY;
import static org.gbif.ipt.config.Constants.VOCAB_KINGDOM;
import static org.gbif.ipt.config.Constants.VOCAB_ORDER;
import static org.gbif.ipt.config.Constants.VOCAB_PHYLUM;

@Singleton
public class ResourceMetadataInferringServiceImpl implements ResourceMetadataInferringService {

  protected final Logger LOG = LogManager.getLogger(ResourceMetadataInferringServiceImpl.class);

  private static final String CAMTRAP_DEPLOYMENTS = "deployments";
  private static final String CAMTRAP_OBSERVATIONS = "observations";
  public static final String CAMTRAP_OBSERVATIONS_TAXON_ID = "taxonID";
  public static final String CAMTRAP_OBSERVATIONS_SCIENTIFIC_NAME = "scientificName";
  public static final String CAMTRAP_DEPLOYMENTS_LATITUDE = "latitude";
  public static final String CAMTRAP_DEPLOYMENTS_LONGITUDE = "longitude";
  public static final String CAMTRAP_DEPLOYMENTS_DEPLOYMENT_START = "deploymentStart";
  public static final String CAMTRAP_DEPLOYMENTS_DEPLOYMENT_END = "deploymentEnd";

  private final SourceManager sourceManager;
  private final VocabulariesManager vocabManager;

  @Inject
  public ResourceMetadataInferringServiceImpl(SourceManager sourceManager, VocabulariesManager vocabManager) {
    this.sourceManager = sourceManager;
    this.vocabManager = vocabManager;
  }

  @Override
  public InferredMetadata inferMetadata(Resource resource) {
    if (resource.isDataPackage()) {
      if (CAMTRAP_DP.equals(resource.getCoreType())) {
        return inferCamtrapMetadata(resource);
      } else {
        LOG.error("Metadata inferring is not supported for the type {}", resource.getCoreType());
        return null;
      }
    } else {
      return inferEmlMetadata(resource);
    }
  }

  private InferredEmlMetadata inferEmlMetadata(Resource resource) {
    InferredEmlMetadata inferredMetadata = new InferredEmlMetadata();
    InferredEmlMetadataParams params = new InferredEmlMetadataParams();

    boolean mappingsExist = mappingsExist(resource, params);

    if (mappingsExist) {
      for (ExtensionMapping mapping : resource.getMappings()) {
        processMapping(mapping, params);
      }
    }

    finalizeInferredMetadata(inferredMetadata, params);
    inferredMetadata.setLastModified(new Date());

    return inferredMetadata;
  }

  private boolean mappingsExist(Resource resource, InferredEmlMetadataParams params) {
    boolean mappingsExist = !resource.getMappings().isEmpty();

    params.geographic.mappingsExist = mappingsExist;
    params.temporal.mappingsExist = mappingsExist;
    params.taxonomic.mappingsExist = mappingsExist;

    return mappingsExist;
  }

  private void finalizeInferredMetadata(InferredEmlMetadata metadata, InferredEmlMetadataParams params) {
    finalizeInferredMetadata(metadata, params.geographic);
    finalizeInferredMetadata(metadata, params.temporal);
    finalizeInferredMetadata(metadata, params.taxonomic);
  }

  private void finalizeInferredMetadata(InferredEmlMetadata metadata, InferredEmlGeographicMetadataParams params) {
    InferredEmlGeographicCoverage inferredGeographicMetadata = new InferredEmlGeographicCoverage();
    metadata.setInferredEmlGeographicCoverage(inferredGeographicMetadata);

    boolean errorOccurredWhileProcessingGeographicMetadata
        = handleEmlGeographicMetadataErrors(inferredGeographicMetadata, params);

    if (!errorOccurredWhileProcessingGeographicMetadata) {
      inferredGeographicMetadata.setInferred(true);
      GeospatialCoverage geospatialCoverage = new GeospatialCoverage();
      geospatialCoverage.setBoundingCoordinates(
          new BBox(
              new Point(params.minDecimalLatitude, params.minDecimalLongitude),
              new Point(params.maxDecimalLatitude, params.maxDecimalLongitude)));
      inferredGeographicMetadata.setData(geospatialCoverage);
    }
  }

  private void finalizeInferredMetadata(InferredEmlMetadata metadata, InferredEmlTemporalMetadataParams params) {
    InferredEmlTemporalCoverage inferredTemporalMetadata = new InferredEmlTemporalCoverage();
    metadata.setInferredEmlTemporalCoverage(inferredTemporalMetadata);

    boolean errorOccurredWhileProcessingGeographicMetadata
        = handleEmlTemporalMetadataErrors(inferredTemporalMetadata, params);

    if (!errorOccurredWhileProcessingGeographicMetadata) {
      TemporalCoverage tempCoverage = new TemporalCoverage();
      try {
        tempCoverage.setStart(params.startDateStr);
        tempCoverage.setEnd(params.endDateStr);
        inferredTemporalMetadata.setInferred(true);
        inferredTemporalMetadata.setData(tempCoverage);
      } catch (ParseException e) {
        LOG.error("Failed to parse date for temporal coverage", e);
        inferredTemporalMetadata.addError("eml.temporalCoverages.error.dateParseException");
      }
    }
  }

  private void finalizeInferredMetadata(InferredEmlMetadata metadata, InferredEmlTaxonomicMetadataParams params) {
    InferredEmlTaxonomicCoverage inferredTaxonomicMetadata = new InferredEmlTaxonomicCoverage();
    metadata.setInferredEmlTaxonomicCoverage(inferredTaxonomicMetadata);

    boolean errorOccurredWhileProcessingTaxonomicMetadata
        = handleEmlTaxonomicMetadataErrors(inferredTaxonomicMetadata, params);

    if (!errorOccurredWhileProcessingTaxonomicMetadata) {
      TaxonomicCoverage taxCoverage = new TaxonomicCoverage();
      taxCoverage.setTaxonKeywords(new ArrayList<>(params.taxa));
      OrganizedTaxonomicCoverage organizedTaxCoverage = constructOrganizedTaxonomicCoverage(taxCoverage);
      inferredTaxonomicMetadata.setInferred(true);
      inferredTaxonomicMetadata.setData(taxCoverage);
      inferredTaxonomicMetadata.setOrganizedData(organizedTaxCoverage);
    }
  }

  private boolean handleEmlGeographicMetadataErrors(
      InferredEmlGeographicCoverage inferredGeographicMetadata,
      InferredEmlGeographicMetadataParams params) {
    boolean errorsPresent = false;

    if (params.serverError) {
      inferredGeographicMetadata.addError("eml.error.serverError");
      errorsPresent = true;
    } else if (!params.mappingsExist) {
      inferredGeographicMetadata.addError("eml.error.noMappings");
      errorsPresent = true;
    } else if (!params.isDecimalLatitudePropertyMapped() || !params.isDecimalLongitudePropertyMapped()) {
      inferredGeographicMetadata.addError("eml.geospatialCoverages.error.fieldsNotMapped");
      errorsPresent = true;
    } else if (params.noValidDataGeo) {
      inferredGeographicMetadata.addError("eml.error.noValidData");
      errorsPresent = true;
    }

    return errorsPresent;
  }

  private boolean handleEmlTemporalMetadataErrors(
      InferredEmlTemporalCoverage inferredTemporalMetadata,
      InferredEmlTemporalMetadataParams params) {
    boolean errorsPresent = false;

    if (params.serverError) {
      inferredTemporalMetadata.addError("eml.error.serverError");
      errorsPresent = true;
    } else if (!params.mappingsExist) {
      inferredTemporalMetadata.addError("eml.error.noMappings");
      errorsPresent = true;
    } else if (!params.isEventDatePropertyMapped()) {
      inferredTemporalMetadata.addError("eml.temporalCoverages.error.fieldsNotMapped");
      errorsPresent = true;
    } else if (params.noValidDataTemporal) {
      inferredTemporalMetadata.addError("eml.error.noValidData");
      errorsPresent = true;
    }

    return errorsPresent;
  }

  private boolean handleEmlTaxonomicMetadataErrors(
      InferredEmlTaxonomicCoverage inferredTaxonomicMetadata,
      InferredEmlTaxonomicMetadataParams params) {
    boolean errorsPresent = false;

    if (params.serverError) {
      inferredTaxonomicMetadata.addError("eml.error.serverError");
      errorsPresent = true;
    } else if (!params.mappingsExist) {
      inferredTaxonomicMetadata.addError("eml.error.noMappings");
      errorsPresent = true;
    } else if (!params.isDataMapped()) {
      inferredTaxonomicMetadata.addError("eml.taxonomicCoverages.error.fieldsNotMapped");
      errorsPresent = true;
    } else if (params.taxonItemsAdded == 0) {
      inferredTaxonomicMetadata.addError("eml.error.noValidData");
      errorsPresent = true;
    }

    return errorsPresent;
  }

  private void processMapping(ExtensionMapping mapping, InferredEmlMetadataParams params) {
    // calculate column indexes for mapping
    for (PropertyMapping field : mapping.getFields()) {
      if (VOCAB_DECIMAL_LONGITUDE.equals(field.getTerm().qualifiedName())) {
        params.geographic.decimalLongitudeSourceColumnIndex = field.getIndex();
      } else if (VOCAB_DECIMAL_LATITUDE.equals(field.getTerm().qualifiedName())) {
        params.geographic.decimalLatitudeSourceColumnIndex = field.getIndex();
      } else if (VOCAB_KINGDOM.equals(field.getTerm().qualifiedName())) {
        params.taxonomic.kingdomSourceColumnIndex = field.getIndex();
      } else if (VOCAB_PHYLUM.equals(field.getTerm().qualifiedName())) {
        params.taxonomic.phylumSourceColumnIndex = field.getIndex();
      } else if (VOCAB_CLASS.equals(field.getTerm().qualifiedName())) {
        params.taxonomic.classSourceColumnIndex = field.getIndex();
      } else if (VOCAB_ORDER.equals(field.getTerm().qualifiedName())) {
        params.taxonomic.orderSourceColumnIndex = field.getIndex();
      } else if (VOCAB_FAMILY.equals(field.getTerm().qualifiedName())) {
        params.taxonomic.familySourceColumnIndex = field.getIndex();
      } else if (VOCAB_EVENT_DATE.equals(field.getTerm().qualifiedName())) {
        params.temporal.eventDateSourceColumnIndex = field.getIndex();
      }
    }

    ClosableReportingIterator<String[]> iter = null;
    try {
      // get the source iterator
      iter = sourceManager.rowIterator(mapping.getSource());
      boolean initializeExtremeValues = true;

      while (iter.hasNext()) {
        String[] in = iter.next();
        if (in == null || in.length == 0) {
          continue;
        }

        processLine(in, params.geographic);
        processLine(in, params.taxonomic);
        processLine(in, params.temporal);
      }
      // Catch ParseException, occurs for Excel files. Find out why
    } catch (com.github.pjfanning.xlsx.exceptions.ParseException e) {
      LOG.error("Error while trying to infer metadata: {}", e.getMessage());
    } catch (Exception e) {
      LOG.error("Error while trying to infer metadata from source data", e);
      params.geographic.serverError = true;
      params.temporal.serverError = true;
      params.taxonomic.serverError = true;
    } finally {
      if (iter != null) {
        try {
          iter.close();
        } catch (Exception e) {
          LOG.error("Error while closing iterator", e);
          params.geographic.serverError = true;
          params.temporal.serverError = true;
          params.taxonomic.serverError = true;
        }
      }
    }
  }

  private void processLine(String[] in, InferredEmlGeographicMetadataParams params) {
    if (params.isDecimalLongitudePropertyMapped()
        && params.isDecimalLatitudePropertyMapped()
        && params.isColumnIndexesWithingRanges(in.length)) {
      String rawLatitudeValue = in[params.decimalLatitudeSourceColumnIndex];
      String rawLongitudeValue = in[params.decimalLongitudeSourceColumnIndex];

      OccurrenceParseResult<LatLng> latLngParseResult =
          CoordinateParseUtils.parseLatLng(rawLatitudeValue, rawLongitudeValue);
      LatLng latLng = latLngParseResult.getPayload();

      // skip erratic records
      if (latLng != null && latLngParseResult.isSuccessful()) {
        params.noValidDataGeo = false;

        // initialize min and max values
        if (!params.coordinatesInitialized) {
          params.minDecimalLatitude = latLng.getLat();
          params.maxDecimalLatitude = latLng.getLat();
          params.minDecimalLongitude = latLng.getLng();
          params.maxDecimalLongitude = latLng.getLng();
          params.coordinatesInitialized = true;
        }

        if (latLng.getLat() > params.maxDecimalLatitude) {
          params.maxDecimalLatitude = latLng.getLat();
        }
        if (latLng.getLat() < params.minDecimalLatitude) {
          params.minDecimalLatitude = latLng.getLat();
        }

        if (latLng.getLng() > params.maxDecimalLongitude) {
          params.maxDecimalLongitude = latLng.getLng();
        }
        if (latLng.getLng() < params.minDecimalLongitude) {
          params.minDecimalLongitude = latLng.getLng();
        }
      }
    }
  }

  private void processLine(String[] in, InferredEmlTemporalMetadataParams params) {
    if (params.isEventDatePropertyMapped() && params.isColumnIndexesWithingRanges(in.length)) {
      String rawEventDateValue = in[params.eventDateSourceColumnIndex];

      TemporalParser temporalParser = DateParsers.defaultTemporalParser();
      ParseResult<TemporalAccessor> parsedEventDateResult = temporalParser.parse(rawEventDateValue);
      TemporalAccessor parsedEventDateTA = parsedEventDateResult.getPayload();

      // skip erratic records
      if (!parsedEventDateResult.isSuccessful() || parsedEventDateTA == null || !parsedEventDateTA.isSupported(ChronoField.YEAR)) {
        return;
      } else {
        params.noValidDataTemporal = false;
      }

      if (params.startDateTA == null) {
        params.startDateTA = parsedEventDateTA;
        params.startDateStr = rawEventDateValue;
      }
      if (params.endDateTA == null) {
        params.endDateTA = parsedEventDateTA;
        params.endDateStr = rawEventDateValue;
      }

      if (parsedEventDateTA instanceof YearMonth) {
        parsedEventDateTA = ((YearMonth) parsedEventDateTA).atEndOfMonth();
      }

      if (parsedEventDateTA instanceof ChronoLocalDate
          && params.startDateTA instanceof ChronoLocalDate
          && ((ChronoLocalDate) params.startDateTA).isAfter((ChronoLocalDate) parsedEventDateTA)) {
        params.startDateTA = parsedEventDateTA;
        params.startDateStr = rawEventDateValue;
      }

      if (parsedEventDateTA instanceof ChronoLocalDate
          && params.endDateTA instanceof ChronoLocalDate
          && ((ChronoLocalDate) params.endDateTA).isBefore((ChronoLocalDate) parsedEventDateTA)) {
        params.endDateTA = parsedEventDateTA;
        params.endDateStr = rawEventDateValue;
      }
    }
  }

  private void processLine(String[] in, InferredEmlTaxonomicMetadataParams params) {
    if (params.isDataMapped() && params.isMaxItemsNumberNotExceeded()) {
      if (params.isKingdomPropertyMapped() && params.isKingdomIndexWithingRange(in.length)) {
        params.addNewTaxon(in[params.kingdomSourceColumnIndex], KINGDOM);
      }

      if (params.isPhylumPropertyMapped() && params.isPhylumIndexWithinRange(in.length)) {
        params.addNewTaxon(in[params.phylumSourceColumnIndex], PHYLUM);
      }

      if (params.isClassPropertyMapped() && params.isClassIndexWithinRange(in.length)) {
        params.addNewTaxon(in[params.classSourceColumnIndex], CLASS);
      }

      if (params.isOrderPropertyMapped() && params.isOrderIndexWithinRange(in.length)) {
        params.addNewTaxon(in[params.orderSourceColumnIndex], ORDER);
      }

      if (params.isFamilyPropertyMapped() && params.isFamilyIndexWithingRange(in.length)) {
        params.addNewTaxon(in[params.familySourceColumnIndex], FAMILY);
      }
    }
  }

  static class InferredEmlMetadataParams {
    private final InferredEmlGeographicMetadataParams geographic = new InferredEmlGeographicMetadataParams();
    private final InferredEmlTemporalMetadataParams temporal = new InferredEmlTemporalMetadataParams();
    private final InferredEmlTaxonomicMetadataParams taxonomic = new InferredEmlTaxonomicMetadataParams();
  }

  static class InferredEmlGeographicMetadataParams {
    private int decimalLongitudeSourceColumnIndex = -1;
    private int decimalLatitudeSourceColumnIndex = -1;
    private boolean mappingsExist;
    private boolean serverError;
    private boolean noValidDataGeo = true;
    private Double minDecimalLongitude = -180.0D;
    private Double maxDecimalLongitude = 180.0D;
    private Double minDecimalLatitude = -90.0D;
    private Double maxDecimalLatitude = 90.0D;
    private boolean coordinatesInitialized;

    public boolean isColumnIndexesWithingRanges(int range) {
      return decimalLongitudeSourceColumnIndex < range && decimalLatitudeSourceColumnIndex < range;
    }

    public boolean isDecimalLongitudePropertyMapped() {
      return decimalLongitudeSourceColumnIndex != -1;
    }

    public boolean isDecimalLatitudePropertyMapped() {
      return decimalLatitudeSourceColumnIndex != -1;
    }
  }

  static class InferredEmlTemporalMetadataParams {
    private int eventDateSourceColumnIndex = -1;
    private boolean mappingsExist;
    private boolean serverError;
    boolean noValidDataTemporal = true;
    private String startDateStr = null;
    private TemporalAccessor startDateTA = null;
    private String endDateStr = null;
    private TemporalAccessor endDateTA = null;

    public boolean isColumnIndexesWithingRanges(int range) {
      return eventDateSourceColumnIndex < range;
    }

    public boolean isEventDatePropertyMapped() {
      return eventDateSourceColumnIndex != -1;
    }
  }

  static class InferredEmlTaxonomicMetadataParams {
    private int kingdomSourceColumnIndex = -1;
    private int phylumSourceColumnIndex = -1;
    private int classSourceColumnIndex = -1;
    private int orderSourceColumnIndex = -1;
    private int familySourceColumnIndex = -1;
    private boolean mappingsExist;
    private boolean serverError;
    private int taxonItemsAdded = 0;
    private final Set<TaxonKeyword> taxa = new HashSet<>();

    public void addNewTaxon(String taxon, String type) {
      if (StringUtils.isNotEmpty(taxon)) {
        taxa.add(new TaxonKeyword(taxon, type, null));
        taxonItemsAdded++;
      }
    }

    public boolean isMaxItemsNumberNotExceeded() {
      return taxonItemsAdded < 200;
    }

    public boolean isDataMapped() {
      return kingdomSourceColumnIndex != -1
          || phylumSourceColumnIndex != -1
          || classSourceColumnIndex != -1
          || orderSourceColumnIndex != -1
          || familySourceColumnIndex != -1;
    }

    public boolean isKingdomPropertyMapped() {
      return kingdomSourceColumnIndex != -1;
    }

    public boolean isKingdomIndexWithingRange(int range) {
      return kingdomSourceColumnIndex < range;
    }

    public boolean isPhylumPropertyMapped() {
      return phylumSourceColumnIndex != -1;
    }

    public boolean isPhylumIndexWithinRange(int range) {
      return phylumSourceColumnIndex < range;
    }

    public boolean isClassPropertyMapped() {
      return classSourceColumnIndex != -1;
    }

    public boolean isClassIndexWithinRange(int range) {
      return classSourceColumnIndex < range;
    }

    public boolean isOrderPropertyMapped() {
      return orderSourceColumnIndex != -1;
    }

    public boolean isOrderIndexWithinRange(int range) {
      return orderSourceColumnIndex < range;
    }

    public boolean isFamilyPropertyMapped() {
      return familySourceColumnIndex != -1;
    }

    public boolean isFamilyIndexWithingRange(int range) {
      return familySourceColumnIndex < range;
    }
  }

  public List<OrganizedTaxonomicCoverage> constructOrganizedTaxonomicCoverages(List<TaxonomicCoverage> coverages) {
    List<OrganizedTaxonomicCoverage> organizedTaxonomicCoverages = new ArrayList<>();
    for (TaxonomicCoverage coverage : coverages) {
      OrganizedTaxonomicCoverage organizedCoverage = constructOrganizedTaxonomicCoverage(coverage);
      organizedTaxonomicCoverages.add(organizedCoverage);
    }
    return organizedTaxonomicCoverages;
  }

  public OrganizedTaxonomicCoverage constructOrganizedTaxonomicCoverage(TaxonomicCoverage coverage) {
    OrganizedTaxonomicCoverage organizedCoverage = new OrganizedTaxonomicCoverage();
    organizedCoverage.setDescription(coverage.getDescription());
    organizedCoverage.setKeywords(setOrganizedTaxonomicKeywords(coverage.getTaxonKeywords()));
    return organizedCoverage;
  }

  private List<OrganizedTaxonomicKeywords> setOrganizedTaxonomicKeywords(List<TaxonKeyword> keywords) {
    List<OrganizedTaxonomicKeywords> organizedTaxonomicKeywordsList = new ArrayList<>();

    // also, we want a unique set of names corresponding to empty rank
    Set<String> uniqueNamesForEmptyRank = new HashSet<>();

    Map<String, String> ranks = new LinkedHashMap<>(vocabManager.getI18nVocab(Constants.VOCAB_URI_RANKS, Locale.ENGLISH.getLanguage(), false));

    for (String rank : ranks.keySet()) {
      OrganizedTaxonomicKeywords organizedKeywords = new OrganizedTaxonomicKeywords();
      // set rank
      organizedKeywords.setRank(rank);
      // construct display name for each TaxonKeyword, and add display name to organized keywords list
      for (TaxonKeyword keyword : keywords) {
        // add display name to appropriate list if it isn't null
        String displayName = createKeywordDisplayName(keyword);
        if (displayName != null) {
          if (rank.equalsIgnoreCase(keyword.getRank())) {
            organizedKeywords.getDisplayNames().add(displayName);
          } else if (StringUtils.trimToNull(keyword.getRank()) == null) {
            uniqueNamesForEmptyRank.add(displayName);
          }
        }
      }
      // add to list
      organizedTaxonomicKeywordsList.add(organizedKeywords);
    }
    // if there were actually some names with empty ranks, add the special OrganizedTaxonomicKeywords for empty rank
    if (!uniqueNamesForEmptyRank.isEmpty()) {
      // create special OrganizedTaxonomicKeywords for empty rank
      OrganizedTaxonomicKeywords emptyRankKeywords = new OrganizedTaxonomicKeywords();
      emptyRankKeywords.setRank("Unranked");
      emptyRankKeywords.setDisplayNames(new ArrayList<>(uniqueNamesForEmptyRank));
      organizedTaxonomicKeywordsList.add(emptyRankKeywords);
    }
    // return list
    return organizedTaxonomicKeywordsList;
  }

  private String createKeywordDisplayName(TaxonKeyword keyword) {
    String combined = null;
    if (keyword != null) {
      String scientificName = StringUtils.trimToNull(keyword.getScientificName());
      String commonName = StringUtils.trimToNull(keyword.getCommonName());
      if (scientificName != null && commonName != null) {
        combined = scientificName + " (" + commonName + ")";
      } else if (scientificName != null) {
        combined = scientificName;
      } else if (commonName != null) {
        combined = commonName;
      }
    }
    return combined;
  }

  private InferredCamtrapMetadata inferCamtrapMetadata(Resource resource) {
    InferredCamtrapMetadata inferredMetadata = new InferredCamtrapMetadata();
    InferredCamtrapMetadataParams params = new InferredCamtrapMetadataParams();

    boolean requiredSchemasMapped = isRequiredSchemasMapped(resource, params);

    if (requiredSchemasMapped) {
      for (DataSchemaMapping mapping : resource.getDataSchemaMappings()) {
        processMapping(mapping, params);
      }
    }

    finalizeInferredMetadata(inferredMetadata, params);
    inferredMetadata.setLastModified(new Date());

    return inferredMetadata;
  }

  private boolean isRequiredSchemasMapped(Resource resource, InferredCamtrapMetadataParams params) {
    boolean isDeploymentsMapped = isSchemaMapped(resource, CAMTRAP_DEPLOYMENTS);
    boolean isObservationsMapped = isSchemaMapped(resource, CAMTRAP_OBSERVATIONS);
    params.geographic.isDeploymentsMapped = isDeploymentsMapped;
    params.temporal.isDeploymentsMapped = isDeploymentsMapped;
    params.taxonomic.isObservationsMapped = isObservationsMapped;

    return isDeploymentsMapped || isObservationsMapped;
  }

  private void processMapping(DataSchemaMapping mapping, InferredCamtrapMetadataParams params) {
    if (CAMTRAP_DEPLOYMENTS.equals(mapping.getDataSchemaFile())) {
      processDeploymentsMapping(mapping, params);
    } else if (CAMTRAP_OBSERVATIONS.equals(mapping.getDataSchemaFile())) {
      processObservationsMapping(mapping, params);
    }
  }

  private void processObservationsMapping(DataSchemaMapping mapping, InferredCamtrapMetadataParams params) {
    // calculate column indexes for mapping
    params.taxonomic.taxonIdSourceColumnIndex = getFieldIndexInMapping(mapping, CAMTRAP_OBSERVATIONS_TAXON_ID);
    params.taxonomic.scientificNameSourceColumnIndex = getFieldIndexInMapping(mapping, CAMTRAP_OBSERVATIONS_SCIENTIFIC_NAME);

    ClosableReportingIterator<String[]> iter = null;
    try {
      // get the source iterator
      iter = sourceManager.rowIterator(mapping.getSource());

      while (iter.hasNext()) {
        String[] in = iter.next();
        if (in == null || in.length == 0) {
          continue;
        }

        processLine(in, params.taxonomic);
      }
      // Catch ParseException, occurs for Excel files. Find out why
    } catch (com.github.pjfanning.xlsx.exceptions.ParseException e) {
      LOG.error("Error while trying to infer metadata: {}", e.getMessage());
    } catch (Exception e) {
      LOG.error("Error while trying to infer metadata from source data", e);
      params.taxonomic.serverError = true;
    } finally {
      if (iter != null) {
        try {
          iter.close();
        } catch (Exception e) {
          LOG.error("Error while closing iterator", e);
          params.taxonomic.serverError = true;
        }
      }
    }
  }

  private void processDeploymentsMapping(DataSchemaMapping mapping, InferredCamtrapMetadataParams params) {
    // calculate column indexes for mapping
    params.geographic.latitudeSourceColumnIndex = getFieldIndexInMapping(mapping, CAMTRAP_DEPLOYMENTS_LATITUDE);
    params.geographic.longitudeSourceColumnIndex = getFieldIndexInMapping(mapping, CAMTRAP_DEPLOYMENTS_LONGITUDE);
    params.temporal.deploymentStartSourceColumnIndex = getFieldIndexInMapping(mapping, CAMTRAP_DEPLOYMENTS_DEPLOYMENT_START);
    params.temporal.deploymentEndSourceColumnIndex = getFieldIndexInMapping(mapping, CAMTRAP_DEPLOYMENTS_DEPLOYMENT_END);

    ClosableReportingIterator<String[]> iter = null;
    try {
      // get the source iterator
      iter = sourceManager.rowIterator(mapping.getSource());

      while (iter.hasNext()) {
        String[] in = iter.next();
        if (in == null || in.length == 0) {
          continue;
        }

        processLine(in, params.geographic);
        processLine(in, params.temporal);
      }
      // Catch ParseException, occurs for Excel files. Find out why
    } catch (com.github.pjfanning.xlsx.exceptions.ParseException e) {
      LOG.error("Error while trying to infer metadata: {}", e.getMessage());
    } catch (Exception e) {
      LOG.error("Error while trying to infer metadata from source data", e);
      params.geographic.serverError = true;
      params.temporal.serverError = true;
    } finally {
      if (iter != null) {
        try {
          iter.close();
        } catch (Exception e) {
          LOG.error("Error while closing iterator", e);
          params.geographic.serverError = true;
          params.temporal.serverError = true;
        }
      }
    }
  }

  private void processLine(String[] in, InferredCamtrapGeographicScopeParams params) {
    if (params.isLatitudeMapped()
        && params.isLongitudeMapped()
        && params.isColumnIndexesWithinRange(in.length)) {
      String rawLatitudeValue = in[params.latitudeSourceColumnIndex];
      String rawLongitudeValue = in[params.longitudeSourceColumnIndex];

      OccurrenceParseResult<LatLng> latLngParseResult =
          CoordinateParseUtils.parseLatLng(rawLatitudeValue, rawLongitudeValue);
      LatLng latLng = latLngParseResult.getPayload();

      // skip erratic records
      // TODO: 23/10/2023 store invalid values?
      if (latLng != null && latLngParseResult.isSuccessful()) {
        params.noValidDataGeo = false;

        // initialize min and max values
        if (!params.coordinatesInitialized) {
          params.minDecimalLatitude = latLng.getLat();
          params.maxDecimalLatitude = latLng.getLat();
          params.minDecimalLongitude = latLng.getLng();
          params.maxDecimalLongitude = latLng.getLng();
          params.coordinatesInitialized = true;
        }

        if (latLng.getLat() > params.maxDecimalLatitude) {
          params.maxDecimalLatitude = latLng.getLat();
        }
        if (latLng.getLat() < params.minDecimalLatitude) {
          params.minDecimalLatitude = latLng.getLat();
        }

        if (latLng.getLng() > params.maxDecimalLongitude) {
          params.maxDecimalLongitude = latLng.getLng();
        }
        if (latLng.getLng() < params.minDecimalLongitude) {
          params.minDecimalLongitude = latLng.getLng();
        }
      }
    }
  }

  private void processLine(String[] in, InferredCamtrapTemporalScopeParams params) {
    if (params.isDeploymentStartMapped()
        && params.isDeploymentEndMapped()
        && params.isColumnIndexesWithinRange(in.length)) {
      String rawDeploymentStartValue = in[params.deploymentStartSourceColumnIndex];
      String rawDeploymentEndValue = in[params.deploymentEndSourceColumnIndex];

      TemporalParser temporalParser = DateParsers.defaultTemporalParser();
      ParseResult<TemporalAccessor> parsedDeploymentStartResult = temporalParser.parse(rawDeploymentStartValue);
      ParseResult<TemporalAccessor> parsedDeploymentEndResult = temporalParser.parse(rawDeploymentEndValue);
      TemporalAccessor parsedDeploymentStartTA = parsedDeploymentStartResult.getPayload();
      TemporalAccessor parsedDeploymentEndTA = parsedDeploymentEndResult.getPayload();

      // skip erratic records
      if (!parsedDeploymentStartResult.isSuccessful() || parsedDeploymentStartTA == null || !parsedDeploymentStartTA.isSupported(ChronoField.YEAR)
          || !parsedDeploymentEndResult.isSuccessful() || parsedDeploymentEndTA == null || !parsedDeploymentEndTA.isSupported(ChronoField.YEAR)) {
        return;
      } else {
        params.noValidDataTemporal = false;
      }

      if (params.startDateTA == null) {
        params.startDateTA = parsedDeploymentStartTA;
        params.startDateStr = rawDeploymentStartValue;
      }
      if (params.endDateTA == null) {
        params.endDateTA = parsedDeploymentEndTA;
        params.endDateStr = rawDeploymentEndValue;
      }

      if (parsedDeploymentStartTA instanceof YearMonth) {
        parsedDeploymentStartTA = ((YearMonth) parsedDeploymentStartTA).atEndOfMonth();
      }

      if (parsedDeploymentEndTA instanceof YearMonth) {
        parsedDeploymentEndTA = ((YearMonth) parsedDeploymentEndTA).atEndOfMonth();
      }

      if (parsedDeploymentStartTA instanceof ChronoLocalDate
          && params.startDateTA instanceof ChronoLocalDate
          && ((ChronoLocalDate) params.startDateTA).isAfter((ChronoLocalDate) parsedDeploymentStartTA)) {
        params.startDateTA = parsedDeploymentStartTA;
        params.startDateStr = rawDeploymentStartValue;
      }

      if (parsedDeploymentEndTA instanceof ChronoLocalDate
          && params.endDateTA instanceof ChronoLocalDate
          && ((ChronoLocalDate) params.endDateTA).isBefore((ChronoLocalDate) parsedDeploymentEndTA)) {
        params.endDateTA = parsedDeploymentEndTA;
        params.endDateStr = rawDeploymentEndValue;
      }
    }
  }

  private void processLine(String[] in, InferredCamtrapTaxonomicScopeParams params) {
    if (params.isTaxonIdMapped()
        && params.isScientificNameMapped()
        && params.taxonItemsAdded < params.maxNumberOfTaxonItems) {
      if (params.isColumnIndexesWithinRange(in.length)
          && StringUtils.isNotEmpty(in[params.taxonIdSourceColumnIndex])
          && StringUtils.isNotEmpty(in[params.scientificNameSourceColumnIndex])) {

        params.taxa.put(in[params.taxonIdSourceColumnIndex], in[params.scientificNameSourceColumnIndex]);
        params.taxonItemsAdded++;
      }
    }
  }

  private void finalizeInferredMetadata(InferredCamtrapMetadata metadata, InferredCamtrapMetadataParams params) {
    finalizeInferredMetadata(metadata, params.geographic);
    finalizeInferredMetadata(metadata, params.temporal);
    finalizeInferredMetadata(metadata, params.taxonomic);
  }

  private void finalizeInferredMetadata(InferredCamtrapMetadata metadata, InferredCamtrapGeographicScopeParams params) {
    InferredCamtrapGeographicScope inferredGeographicScope = new InferredCamtrapGeographicScope();
    metadata.setInferredGeographicScope(inferredGeographicScope);
    boolean errorOccurredWhileProcessingGeographicMetadata
        = handleCamtrapGeographicScopeErrors(inferredGeographicScope, params);

    if (!errorOccurredWhileProcessingGeographicMetadata) {
      inferredGeographicScope.setMinLatitude(params.minDecimalLatitude);
      inferredGeographicScope.setMinLongitude(params.minDecimalLongitude);
      inferredGeographicScope.setMaxLatitude(params.maxDecimalLatitude);
      inferredGeographicScope.setMaxLongitude(params.maxDecimalLongitude);
      inferredGeographicScope.setInferred(true);
    }
  }

  private void finalizeInferredMetadata(InferredCamtrapMetadata metadata, InferredCamtrapTemporalScopeParams params) {
    InferredCamtrapTemporalScope inferredTemporalScope = new InferredCamtrapTemporalScope();
    metadata.setInferredTemporalScope(inferredTemporalScope);

    boolean errorOccurredWhileProcessingTemporalMetadata
        = handleCamtrapTemporalScopeErrors(inferredTemporalScope, params);

    if (!errorOccurredWhileProcessingTemporalMetadata) {
      try {
        inferredTemporalScope.setStartDate(DateUtils.calendarDate(params.startDateStr));
        inferredTemporalScope.setEndDate(DateUtils.calendarDate(params.endDateStr));
        inferredTemporalScope.setInferred(true);
      } catch (ParseException e) {
        LOG.error("Failed to parse date for temporal coverage", e);
        inferredTemporalScope.addError("datapackagemetadata.temporal.error.dateParseException");
      }
    }
  }

  private void finalizeInferredMetadata(InferredCamtrapMetadata metadata, InferredCamtrapTaxonomicScopeParams params) {
    InferredCamtrapTaxonomicScope inferredTaxonomicScope = new InferredCamtrapTaxonomicScope();
    metadata.setInferredTaxonomicScope(inferredTaxonomicScope);
    boolean errorOccurredWhileProcessingTaxonomicMetadata
        = handleCamtrapTaxonomicScopeErrors(inferredTaxonomicScope, params);

    if (errorOccurredWhileProcessingTaxonomicMetadata) {
      List<Taxonomic> taxCoverage = params.taxa.entrySet().stream()
          .map(entry -> {
            Taxonomic t = new Taxonomic();
            t.setTaxonID(entry.getKey());
            t.setScientificName(entry.getValue());
            return t;
          })
          .collect(Collectors.toList());
      inferredTaxonomicScope.setData(taxCoverage);
      inferredTaxonomicScope.setInferred(true);
    }
  }

  private boolean isSchemaMapped(Resource resource, String schemaName) {
    return resource.getDataSchemaMappings().stream()
        .map(DataSchemaMapping::getDataSchemaFile)
        .anyMatch(schemaName::equals);
  }

  private int getFieldIndexInMapping(DataSchemaMapping mapping, String fieldName) {
    return Optional.ofNullable(mapping.getField(fieldName))
        .map(DataSchemaFieldMapping::getIndex)
        .orElse(-1);
  }

  private boolean handleCamtrapGeographicScopeErrors(
      InferredCamtrapGeographicScope metadata,
      InferredCamtrapGeographicScopeParams params) {
    boolean errorsPresent = false;

    if (params.serverError) {
      metadata.addError("datapackagemetadata.error.serverError");
      errorsPresent = true;
    } else if (!params.isDeploymentsMapped) {
      metadata.addError("datapackagemetadata.error.noDeploymentsMapped");
      errorsPresent = true;
    } else if (!params.isLatitudeMapped() && !params.isLongitudeMapped()) {
      metadata.addError("datapackagemetadata.geographic.error.fieldsNotMapped");
      errorsPresent = true;
    } else if (!params.isLatitudeMapped()) {
      metadata.addError("datapackagemetadata.geographic.error.latitudeNotMapped");
      errorsPresent = true;
    } else if (!params.isLongitudeMapped()) {
      metadata.addError("datapackagemetadata.geographic.error.longitudeNotMapped");
      errorsPresent = true;
    } else if (params.noValidDataGeo) {
      metadata.addError("datapackagemetadata.error.noValidData");
      errorsPresent = true;
    }

    return errorsPresent;
  }

  private boolean handleCamtrapTaxonomicScopeErrors(
      InferredCamtrapTaxonomicScope metadata,
      InferredCamtrapTaxonomicScopeParams params) {
    boolean errorOccurred = false;

    if (params.serverError) {
      metadata.addError("datapackagemetadata.error.serverError");
      errorOccurred = true;
    } else if (!params.isObservationsMapped) {
      metadata.addError("datapackagemetadata.error.noObservationsMapped");
      errorOccurred = true;
    } else if (!params.isTaxonIdMapped() && !params.isScientificNameMapped()) {
      metadata.addError("datapackagemetadata.taxonomic.error.fieldsNotMapped");
      errorOccurred = true;
    } else if (!params.isTaxonIdMapped()) {
      metadata.addError("datapackagemetadata.taxonomic.error.taxonIdNotMapped");
      errorOccurred = true;
    } else if (!params.isScientificNameMapped()) {
      metadata.addError("datapackagemetadata.taxonomic.error.scientificNameNotMapped");
      errorOccurred = true;
    } else if (params.taxonItemsAdded == 0) {
      metadata.addError("datapackagemetadata.error.noValidData");
      errorOccurred = true;
    }

    return errorOccurred;
  }

  private boolean handleCamtrapTemporalScopeErrors(
      InferredCamtrapTemporalScope metadata,
      InferredCamtrapTemporalScopeParams params) {
    boolean errorOccurred = false;

    if (params.serverError) {
      metadata.addError("datapackagemetadata.error.serverError");
      errorOccurred = true;
    } else if (!params.isDeploymentsMapped) {
      metadata.addError("datapackagemetadata.error.noDeployments");
      errorOccurred = true;
    } else if (!params.isDeploymentStartMapped() && !params.isDeploymentEndMapped()) {
      metadata.addError("datapackagemetadata.temporal.error.fieldsNotMapped");
      errorOccurred = true;
    } else if (!params.isDeploymentStartMapped()) {
      metadata.addError("datapackagemetadata.temporal.error.deploymentStartNotMapped");
      errorOccurred = true;
    } else if (!params.isDeploymentEndMapped()) {
      metadata.addError("datapackagemetadata.temporal.error.deploymentEndNotMapped");
      errorOccurred = true;
    } else if (params.noValidDataTemporal) {
      metadata.addError("datapackagemetadata.error.noValidData");
      errorOccurred = true;
    }

    return errorOccurred;
  }

  static class InferredCamtrapMetadataParams {
    private final InferredCamtrapGeographicScopeParams geographic = new InferredCamtrapGeographicScopeParams();
    private final InferredCamtrapTemporalScopeParams temporal = new InferredCamtrapTemporalScopeParams();
    private final InferredCamtrapTaxonomicScopeParams taxonomic = new InferredCamtrapTaxonomicScopeParams();
  }

  static class InferredCamtrapGeographicScopeParams {
    private Double minDecimalLongitude = -180.0D;
    private Double maxDecimalLongitude = 180.0D;
    private Double minDecimalLatitude = -90.0D;
    private Double maxDecimalLatitude = 90.0D;
    private boolean coordinatesInitialized;
    private int longitudeSourceColumnIndex = -1;
    private int latitudeSourceColumnIndex = -1;
    private boolean serverError;
    private boolean isDeploymentsMapped;
    private boolean noValidDataGeo = true;

    public boolean isLatitudeMapped() {
      return latitudeSourceColumnIndex != -1;
    }

    public boolean isLongitudeMapped() {
      return longitudeSourceColumnIndex != -1;
    }

    public boolean isColumnIndexesWithinRange(int range) {
      return longitudeSourceColumnIndex < range && latitudeSourceColumnIndex < range;
    }
  }

  static class InferredCamtrapTaxonomicScopeParams {
    private int taxonItemsAdded = 0;
    private final int maxNumberOfTaxonItems = 200;
    private final Map<String, String> taxa = new HashMap<>();
    private int taxonIdSourceColumnIndex = -1;
    private int scientificNameSourceColumnIndex = -1;
    private boolean serverError;
    private boolean isObservationsMapped;
    private boolean noValidDataGeo;

    public boolean isColumnIndexesWithinRange(int range) {
      return taxonIdSourceColumnIndex < range && scientificNameSourceColumnIndex < range;
    }

    public boolean isTaxonIdMapped() {
      return taxonIdSourceColumnIndex != -1;
    }

    public boolean isScientificNameMapped() {
      return scientificNameSourceColumnIndex != -1;
    }
  }

  static class InferredCamtrapTemporalScopeParams {
    private String startDateStr = null;
    private TemporalAccessor startDateTA = null;
    private String endDateStr = null;
    private TemporalAccessor endDateTA = null;
    private int deploymentStartSourceColumnIndex = -1;
    private int deploymentEndSourceColumnIndex = -1;
    private boolean serverError;
    private boolean isDeploymentsMapped;
    private boolean noValidDataTemporal = true;

    public boolean isColumnIndexesWithinRange(int range) {
      return deploymentEndSourceColumnIndex < range && deploymentStartSourceColumnIndex < range;
    }

    public boolean isDeploymentStartMapped() {
      return deploymentStartSourceColumnIndex != -1;
    }

    public boolean isDeploymentEndMapped() {
      return deploymentEndSourceColumnIndex != -1;
    }
  }
}