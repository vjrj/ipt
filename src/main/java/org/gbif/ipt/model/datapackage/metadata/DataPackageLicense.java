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
package org.gbif.ipt.model.datapackage.metadata;

import org.gbif.ipt.validation.BasicMetadata;
import org.gbif.ipt.validation.NotNullIfAnotherFieldNull;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;
import javax.validation.Valid;
import javax.validation.constraints.Pattern;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * License
 * <p>
 * A license for this descriptor.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@NotNullIfAnotherFieldNull(
    fieldName = "name",
    dependFieldName = "path",
    message = "validation.datapackage.metadata.license.nameOrPath.required",
    groups = BasicMetadata.class)
public class DataPackageLicense implements License, Serializable {

  private final static long serialVersionUID = 5529108333342991396L;

  /**
   * Open Definition license identifier
   * <p>
   * MUST be an Open Definition license identifier, see <a href="http://licenses.opendefinition.org/">...</a>
   */
  @JsonProperty("name")
  @Pattern(regexp = "^([-a-zA-Z0-9._])+$", groups = BasicMetadata.class)
  private String name;

  /**
   * Path
   * <p>
   * A fully qualified URL, or a POSIX file path.
   */
  @JsonProperty("path")
  @Pattern(regexp = "^(?=^[^./~])(^((?!\\.{2}).)*$).*$", groups = BasicMetadata.class)
  private String path;

  /**
   * Title
   * <p>
   * A human-readable title.
   */
  @JsonProperty("title")
  private String title;

  @SuppressWarnings("FieldMayBeFinal")
  @JsonIgnore
  @Valid
  private Map<String, Object> additionalProperties = new HashMap<>();

  /**
   * Open Definition license identifier
   * <p>
   * MUST be an Open Definition license identifier, see <a href="http://licenses.opendefinition.org/">...</a>
   */
  @JsonProperty("name")
  public String getName() {
    return name;
  }

  /**
   * Open Definition license identifier
   * <p>
   * MUST be an Open Definition license identifier, see <a href="http://licenses.opendefinition.org/">...</a>
   */
  @JsonProperty("name")
  public void setName(String name) {
    this.name = name;
  }

  /**
   * Path
   * <p>
   * A fully qualified URL, or a POSIX file path.
   */
  @JsonProperty("path")
  public String getPath() {
    return path;
  }

  /**
   * Path
   * <p>
   * A fully qualified URL, or a POSIX file path.
   */
  @JsonProperty("path")
  public void setPath(String path) {
    this.path = path;
  }

  /**
   * Title
   * <p>
   * A human-readable title.
   */
  @JsonProperty("title")
  public String getTitle() {
    return title;
  }

  /**
   * Title
   * <p>
   * A human-readable title.
   */
  @JsonProperty("title")
  public void setTitle(String title) {
    this.title = title;
  }

  @JsonAnyGetter
  public Map<String, Object> getAdditionalProperties() {
    return this.additionalProperties;
  }

  @JsonAnySetter
  public void setAdditionalProperty(String name, Object value) {
    this.additionalProperties.put(name, value);
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", License.class.getSimpleName() + "[", "]")
        .add("name='" + name + "'")
        .add("path='" + path + "'")
        .add("title='" + title + "'")
        .add("additionalProperties=" + additionalProperties)
        .toString();
  }
}
