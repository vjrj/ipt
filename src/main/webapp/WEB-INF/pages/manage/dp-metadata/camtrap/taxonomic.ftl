<#-- @ftlvariable name="" type="org.gbif.ipt.action.manage.DataPackageMetadataAction" -->
<#escape x as x?html>
    <#include "/WEB-INF/pages/inc/header.ftl">
    <title><@s.text name='manage.metadata.basic.title'/></title>
    <script src="${baseURL}/js/jconfirmation.jquery.js"></script>
    <script>
        $(document).ready(function(){
            var taxonItems = calcNumberOfItems("taxon");

            function calcNumberOfItems(name) {
                var lastItem = $("#" + name + "-items > .item:last-child").attr("id");
                if (lastItem !== undefined)
                    return parseInt(lastItem.split("-")[2]);
                else
                    return -1;
            }

            function calcNumberOfVernacularNamesInSection(index) {
                var lastItem = $("#vernacularName-items-" + index + " .item:last-child").attr("id");
                if (lastItem !== undefined)
                    return parseInt(lastItem.split("-")[3]);
                else
                    return -1;
            }

            $("#plus-taxon").click(function (event) {
                event.preventDefault();
                addNewTaxonItem(true);
            });

            $("[id^='plus-vernacularName']").each(function (index, element) {
                var itemId = $(this)[0].id;
                var itemIndex = itemId.split("-")[2];

                $(this).click(function (event) {
                    event.preventDefault();
                    addNewVernacularNameItem(itemIndex, true);
                });
            });

            function addNewTaxonItem(effects) {
                var newItem = $('#baseItem-taxon').clone();
                if (effects) newItem.hide();
                newItem.appendTo('#taxon-items');

                if (effects) {
                    newItem.slideDown('slow');
                }

                setTaxonItemIndex(newItem, ++taxonItems);

                initInfoPopovers(newItem[0]);
            }

            function addNewVernacularNameItem(index, effects) {
                // calculate number of vernacular names in the item
                var subIndex = calcNumberOfVernacularNamesInSection(index) + 1;
                var newItem = $('#baseItem-vernacularName').clone();
                if (effects) newItem.hide();
                newItem.appendTo('#vernacularName-items-' + index);

                if (effects) {
                    newItem.slideDown('slow');
                }

                setVernacularNameItemIndex(newItem, index, subIndex);
            }

            function removeTaxonItem(event) {
                event.preventDefault();
                var $target = $(event.target);
                if (!$target.is('a')) {
                    $target = $(event.target).closest('a');
                }
                $('#taxon-item-' + $target.attr("id").split("-")[2]).slideUp('slow', function () {
                    $(this).remove();
                    $("#taxon-items > .item").each(function (index) {
                        setTaxonItemIndex($(this), index);
                    });
                    calcNumberOfItems("taxon");
                });
            }

            function removeVernacularNameItem(event) {
                event.preventDefault();
                var $target = $(event.target);
                if (!$target.is('a')) {
                    $target = $(event.target).closest('a');
                }
                var elementId = $target.attr("id");
                var elementIdSplit = elementId.split("-");
                var elementIndex = elementIdSplit[2];
                var elementSubIndex = elementIdSplit[3];
                $('#vernacularName-item-' + elementIndex + '-' + elementSubIndex).slideUp('slow', function () {
                    $(this).remove();
                    $("#vernacularName-items-" + elementIndex + " .item").each(function (subIndex) {
                        setVernacularNameItemIndex($(this), elementIndex, subIndex)
                    });
                });
            }

            function setTaxonItemIndex(item, index) {
                item.attr("id", "taxon-item-" + index);

                $("#taxon-item-" + index + " [id^='taxon-removeLink']").attr("id", "taxon-removeLink-" + index);
                $("#taxon-removeLink-" + index).click(function (event) {
                    removeTaxonItem(event);
                });

                $("#taxon-item-" + index + " [id$='taxonID']").attr("id", "metadata.taxonomic[" + index + "].taxonID").attr("name", function () {
                    return $(this).attr("id");
                });
                $("#taxon-item-" + index + " [for$='taxonID']").attr("for", "metadata.taxonomic[" + index + "].taxonID");

                $("#taxon-item-" + index + " [id$='taxonIDReference']").attr("id", "metadata.taxonomic[" + index + "].taxonIDReference").attr("name", function () {
                    return $(this).attr("id");
                });
                $("#taxon-item-" + index + " [for$='taxonIDReference']").attr("for", "metadata.taxonomic[" + index + "].taxonIDReference");

                $("#taxon-item-" + index + " [id$='scientificName']").attr("id", "metadata.taxonomic[" + index + "].scientificName").attr("name", function () {
                    return $(this).attr("id");
                });
                $("#taxon-item-" + index + " [for$='scientificName']").attr("for", "metadata.taxonomic[" + index + "].scientificName");

                $("#taxon-item-" + index + " [id$='taxonRank']").attr("id", "metadata.taxonomic[" + index + "].taxonRank").attr("name", function () {
                    return $(this).attr("id");
                });
                $("#taxon-item-" + index + " [for$='taxonRank']").attr("for", "metadata.taxonomic[" + index + "].taxonRank");

                $("#taxon-item-" + index + " [id$='kingdom']").attr("id", "metadata.taxonomic[" + index + "].kingdom").attr("name", function () {
                    return $(this).attr("id");
                });
                $("#taxon-item-" + index + " [for$='kingdom']").attr("for", "metadata.taxonomic[" + index + "].kingdom");

                $("#taxon-item-" + index + " [id$='phylum']").attr("id", "metadata.taxonomic[" + index + "].phylum").attr("name", function () {
                    return $(this).attr("id");
                });
                $("#taxon-item-" + index + " [for$='phylum']").attr("for", "metadata.taxonomic[" + index + "].phylum");

                $("#taxon-item-" + index + " [id$='class']").attr("id", "metadata.taxonomic[" + index + "].class").attr("name", function () {
                    return $(this).attr("id");
                });
                $("#taxon-item-" + index + " [for$='class']").attr("for", "metadata.taxonomic[" + index + "].class");

                $("#taxon-item-" + index + " [id$='order']").attr("id", "metadata.taxonomic[" + index + "].order").attr("name", function () {
                    return $(this).attr("id");
                });
                $("#taxon-item-" + index + " [for$='order']").attr("for", "metadata.taxonomic[" + index + "].order");

                $("#taxon-item-" + index + " [id$='family']").attr("id", "metadata.taxonomic[" + index + "].family").attr("name", function () {
                    return $(this).attr("id");
                });
                $("#taxon-item-" + index + " [for$='family']").attr("for", "metadata.taxonomic[" + index + "].family");

                $("#taxon-item-" + index + " [id$='genus']").attr("id", "metadata.taxonomic[" + index + "].genus").attr("name", function () {
                    return $(this).attr("id");
                });
                $("#taxon-item-" + index + " [for$='genus']").attr("for", "metadata.taxonomic[" + index + "].genus");

                // Reset vernacular names indexes
                var vernacularNamesBlock = $("#taxon-item-" + index + " .vernacularName-items-wrapper")
                vernacularNamesBlock.attr("id", "vernacularName-items-" + index);

                // Set "add new" id and initialize functionality
                $("#taxon-item-" + index + " #plus-vernacularName")
                    .attr("id", "plus-vernacularName-" + index);

                $("#plus-vernacularName-" + index).click(function (event) {
                    event.preventDefault();
                    addNewVernacularNameItem(index, true);
                });

                $("#vernacularName-items-" + index + " .item").each(function (subIndex) {
                    setVernacularNameItemIndex($(this), index, subIndex)
                });
            }


            function setVernacularNameItemIndex(item, index, subIndex) {
                item.attr("id", "vernacularName-item-" + index + "-" + subIndex);

                $("#vernacularName-item-" + index + "-" + subIndex + " [id^='vernacularName-removeLink']")
                    .attr("id", "vernacularName-removeLink-" + index + "-" + subIndex);
                $("#vernacularName-removeLink-" + index + "-" + subIndex)
                    .click(function (event) {
                        removeVernacularNameItem(event);
                    });

                $("#taxon-item-" + index + " [id^='plus-vernacularName']")
                    .attr("id", "plus-vernacularName-" + index);

                var inputVernacularNameKey = $("#vernacularName-item-" + index + "-" + subIndex + " [id^='vernacularNames-key']");

                inputVernacularNameKey
                    .attr("id", "vernacularNames-key-" + index + "-" + subIndex)
                    .attr("name", function () {return $(this).attr("id");});

                var inputVernacularNameValue = $("#vernacularName-item-" + index + "-" + subIndex + " [id$='value']");

                inputVernacularNameValue.attr("id", "metadata.taxonomic[" + index + "].vernacularNames[" + subIndex + "].value");

                if (inputVernacularNameKey.val()) {
                    inputVernacularNameValue
                        .attr("name", "metadata.taxonomic[" + index + "].vernacularNames['" +  inputVernacularNameKey.val().replaceAll("'", "\'") + "']");
                }

                $("#vernacularNames-key-" + index + "-" + subIndex).change(function() {
                    var newValue = $(this).val();
                    $("#metadata\\.taxonomic\\[" + index + "\\]\\.vernacularNames\\[" + subIndex + "\\]\\.value")
                        .attr("name", "metadata.taxonomic[" + index + "].vernacularNames['" + newValue.replaceAll("'", "\'") + "']");
                });
            }

            $(".removeTaxonLink").click(function (event) {
                removeTaxonItem(event);
            });

            $(".removeVernacularNameLink").click(function (event) {
                removeVernacularNameItem(event);
            });

            $("[id^='vernacularNames-key-']").change(function () {
                var value = $(this).val();
                var itemId = $(this).attr("id");
                var splitItemId = itemId.split("-");
                var itemIndex = splitItemId[2];
                var itemSubIndex = splitItemId[3];

                $("#metadata\\.taxonomic\\[" + itemIndex + "\\]\\.vernacularNames\\[" + itemSubIndex + "\\]\\.value").attr("name", "metadata.taxonomic[" + itemIndex + "].vernacularNames['" + value.replaceAll("'", "\'") + "']");
            });

            // scroll to the error if present
            var invalidElements = $(".is-invalid");

            if (invalidElements !== undefined && invalidElements.length > 0) {
                var invalidElement = invalidElements.first();
                var pos = invalidElement.offset().top - 100;
                // scroll to the element
                $('body, html').animate({scrollTop: pos});
            }
        });
    </script>
    <#assign currentMenu="manage"/>
    <#assign currentMetadataPage = "taxonomic"/>
    <#include "/WEB-INF/pages/inc/menu.ftl">
    <#include "/WEB-INF/pages/macros/forms.ftl"/>

    <form id="taxonomic-metadata-form" class="needs-validation" action="camtrap-metadata-${section}.do" method="post" novalidate>
        <div class="container-fluid bg-body border-bottom">
            <div class="container pt-2">
                <#include "/WEB-INF/pages/inc/action_alerts.ftl">
            </div>

            <div class="container my-3 p-3">
                <div class="text-center text-uppercase fw-bold fs-smaller-2">
                    <div class="text-center text-uppercase fw-bold fs-smaller-2">
                        <nav style="--bs-breadcrumb-divider: url(&#34;data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='8' height='8'%3E%3Cpath d='M2.5 0L1 1.5 3.5 4 1 6.5 2.5 8l4-4-4-4z' fill='currentColor'/%3E%3C/svg%3E&#34;);" aria-label="breadcrumb">
                            <ol class="breadcrumb justify-content-center mb-0">
                                <li class="breadcrumb-item"><a href="/manage/"><@s.text name="breadcrumb.manage"/></a></li>
                                <li class="breadcrumb-item"><a href="resource?r=${resource.shortname}"><@s.text name="breadcrumb.manage.overview"/></a></li>
                                <li class="breadcrumb-item active" aria-current="page"><@s.text name="breadcrumb.manage.overview.metadata"/></li>
                            </ol>
                        </nav>
                    </div>
                </div>

                <div class="text-center">
                    <h1 class="py-2 mb-0 text-gbif-header fs-2 fw-normal">
                        <@s.text name='manage.datapackagemetadata.taxonomic.title'/>
                    </h1>
                </div>

                <div class="text-center fs-smaller">
                    <a href="resource.do?r=${resource.shortname}" title="${resource.title!resource.shortname}">
                        <#if resource.title?has_content>
                            ${resource.title}
                        <#else>
                            ${resource.shortname}
                        </#if>
                    </a>
                </div>

                <div class="text-center mt-2">
                    <@s.submit cssClass="button btn btn-sm btn-outline-gbif-primary top-button" name="save" key="button.save"/>
                    <@s.submit cssClass="button btn btn-sm btn-outline-secondary top-button" name="cancel" key="button.back"/>
                </div>
            </div>
        </div>

        <#--        <#include "metadata_section_select.ftl"/>-->

        <div class="container-fluid bg-body">
            <div class="container bd-layout">

                <main class="bd-main bd-main">
                    <div class="bd-toc mt-4 mb-5 ps-3 mb-lg-5 text-muted">
                        <#include "metadata_sidebar.ftl"/>
                    </div>

                    <div class="bd-content">
                        <div class="my-md-3 p-3">
                            <p class="mb-4"><@s.text name="datapackagemetadata.taxonomic.intro"/></p>

                            <#assign removeTaxonLink><@s.text name='manage.metadata.removethis'/> <@s.text name='datapackagemetadata.taxon'/></#assign>
                            <#assign addTaxonLink><@s.text name='manage.metadata.addnew'/> <@s.text name='datapackagemetadata.taxon'/></#assign>
                            <#assign removeVernacularNameLink><@s.text name='manage.metadata.removethis'/> <@s.text name='datapackagemetadata.taxonomic.vernacularName'/></#assign>
                            <#assign addVernacularNameLink><@s.text name='manage.metadata.addnew'/> <@s.text name='datapackagemetadata.taxonomic.vernacularName'/></#assign>

                            <!-- List of Sources -->
                            <div>
                                <div id="taxon-items">
                                    <#if (metadata.taxonomic)?has_content>
                                        <#list metadata.taxonomic as item>
                                            <div id="taxon-item-${item_index}" class="item clearfix row g-3 border-bottom pb-3 mt-1">
                                                <div class="columnLinks mt-2 d-flex justify-content-end">
                                                    <a id="taxon-removeLink-${item_index}" href="" class="removeTaxonLink metadata-action-link">
                                                        <span>
                                                            <svg viewBox="0 0 24 24" style="fill: #4BA2CE;height: 1em;vertical-align: -0.125em !important;">
                                                                <path d="M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM8 9h8v10H8V9zm7.5-5-1-1h-5l-1 1H5v2h14V4h-3.5z"></path>
                                                            </svg>
                                                        </span>
                                                        <span>${removeTaxonLink?lower_case?cap_first}</span>
                                                    </a>
                                                </div>
                                                <div class="col-lg-6">
                                                    <@input name="metadata.taxonomic[${item_index}].taxonID" help="i18n" i18nkey="datapackagemetadata.taxonomic.taxonId" requiredField=true />
                                                </div>
                                                <div class="col-lg-6">
                                                    <@input name="metadata.taxonomic[${item_index}].taxonIDReference" help="i18n" i18nkey="datapackagemetadata.taxonomic.taxonIdReference" requiredField=true />
                                                </div>
                                                <div class="col-lg-6">
                                                    <@input name="metadata.taxonomic[${item_index}].scientificName" help="i18n" i18nkey="datapackagemetadata.taxonomic.scientificName" requiredField=true />
                                                </div>
                                                <div class="col-lg-6">
                                                    <#if (metadata.taxonomic[item_index].taxonRank)??>
                                                        <@select name="metadata.taxonomic[${item_index}].taxonRank" help="i18n" includeEmpty=true compareValues=true options=taxonRanks i18nkey="datapackagemetadata.taxonomic.taxonRank" value="${metadata.taxonomic[item_index].taxonRank!}"/>
                                                    <#else>
                                                        <@select name="metadata.taxonomic[${item_index}].taxonRank" help="i18n" includeEmpty=true compareValues=true options=taxonRanks i18nkey="datapackagemetadata.taxonomic.taxonRank" value=""/>
                                                    </#if>
                                                </div>

                                                <div class="col-lg-6">
                                                    <@input name="metadata.taxonomic[${item_index}].kingdom" help="i18n" i18nkey="datapackagemetadata.taxonomic.kingdom" />
                                                </div>
                                                <div class="col-lg-6">
                                                    <@input name="metadata.taxonomic[${item_index}].phylum" help="i18n" i18nkey="datapackagemetadata.taxonomic.phylum" />
                                                </div>
                                                <div class="col-lg-6">
                                                    <@input name="metadata.taxonomic[${item_index}].class" help="i18n" i18nkey="datapackagemetadata.taxonomic.class" />
                                                </div>
                                                <div class="col-lg-6">
                                                    <@input name="metadata.taxonomic[${item_index}].order" help="i18n" i18nkey="datapackagemetadata.taxonomic.order" />
                                                </div>
                                                <div class="col-lg-6">
                                                    <@input name="metadata.taxonomic[${item_index}].family" help="i18n" i18nkey="datapackagemetadata.taxonomic.family" />
                                                </div>
                                                <div class="col-lg-6">
                                                    <@input name="metadata.taxonomic[${item_index}].genus" help="i18n" i18nkey="datapackagemetadata.taxonomic.genus" />
                                                </div>

                                                <div class="col-12">
                                                    <span class="form-label">
                                                        <a tabindex="0" role="button" class="popover-link" data-bs-toggle="popover" data-bs-trigger="focus" data-bs-html="true" data-bs-content="<@s.text name='datapackagemetadata.taxonomic.vernacularNames.help'/>" data-bs-original-title="" title="">
                                                            <i class="bi bi-info-circle text-gbif-primary px-1"></i>
                                                        </a>
                                                        <@s.text name="datapackagemetadata.taxonomic.vernacularNames"/>
                                                    </span>
                                                </div>

                                                <div id="vernacularName-items-${item_index}" class="col-12 mt-0 vernacularName-items-wrapper">
                                                    <#if metadata.taxonomic[item_index].vernacularNames?has_content>
                                                        <#list metadata.taxonomic[item_index].vernacularNames?keys as vernacularNameKey>
                                                            <div id="vernacularName-item-${item_index}-${vernacularNameKey_index}" class="row g-3 <#if vernacularNameKey_index != 0>mt-1</#if> item">
                                                                <div class="columnLinks mt-2 d-flex justify-content-end">
                                                                    <a id="vernacularName-removeLink-${item_index}-${vernacularNameKey_index}" href="" class="removeVernacularNameLink metadata-action-link">
                                                                        <span>
                                                                            <svg viewBox="0 0 24 24" style="fill: #4BA2CE;height: 1em;vertical-align: -0.125em !important;">
                                                                                <path d="M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM8 9h8v10H8V9zm7.5-5-1-1h-5l-1 1H5v2h14V4h-3.5z"></path>
                                                                            </svg>
                                                                        </span>
                                                                        <span>${removeVernacularNameLink?lower_case?cap_first}</span>
                                                                    </a>
                                                                </div>
                                                                <div class="col-lg-6">
                                                                  <label for="metadata.taxonomic" class="form-label">
                                                                    Language code
                                                                  </label>
                                                                  <input class="form-control" type="text" id="vernacularNames-key-${item_index}-${vernacularNameKey_index}" name="vernacularNames-key-${item_index}-${vernacularNameKey_index}" value="${vernacularNameKey}">
                                                                </div>
                                                                <div class="col-lg-6">
                                                                  <label for="metadata.taxonomic" class="form-label">
                                                                    Vernacular name
                                                                  </label>
                                                                  <input class="form-control" type="text" id="metadata.taxonomic[${item_index}].vernacularNames[${vernacularNameKey_index}].value" name="metadata.taxonomic[${item_index}].vernacularNames['${vernacularNameKey}']" value="${metadata.taxonomic[item_index].vernacularNames[vernacularNameKey]}">
                                                                </div>
                                                            </div>
                                                        </#list>
                                                    </#if>
                                                </div>

                                                <div class="addNew col-12 mt-2">
                                                    <a id="plus-vernacularName-${item_index}" class="metadata-action-link" href="">
                                                        <span>
                                                            <svg viewBox="0 0 24 24" style="fill: #4BA2CE;height: 1em;vertical-align: -0.125em !important;">
                                                                <path d="M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z"></path>
                                                            </svg>
                                                        </span>
                                                        <span>${addVernacularNameLink?lower_case?cap_first}</span>
                                                    </a>
                                                </div>

                                            </div>
                                        </#list>
                                    </#if>
                                </div>
                                <div class="addNew col-12 mt-2">
                                    <a id="plus-taxon" class="metadata-action-link" href="">
                                        <span>
                                            <svg viewBox="0 0 24 24" style="fill: #4BA2CE;height: 1em;vertical-align: -0.125em !important;">
                                                <path d="M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z"></path>
                                            </svg>
                                        </span>
                                        <span>${addTaxonLink?lower_case?cap_first}</span>
                                    </a>
                                </div>
                            </div>
                        </div>
                    </div>
                </main>
            </div>
        </div>
    </form>

    <div id="baseItem-taxon" class="item clearfix row g-3 border-bottom pb-3 mt-1" style="display: none;">
        <div class="columnLinks mt-2 d-flex justify-content-end">
            <a id="taxon-removeLink" href="" class="removeTaxonLink metadata-action-link">
                <span>
                    <svg viewBox="0 0 24 24" style="fill: #4BA2CE;height: 1em;vertical-align: -0.125em !important;">
                        <path d="M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM8 9h8v10H8V9zm7.5-5-1-1h-5l-1 1H5v2h14V4h-3.5z"></path>
                    </svg>
                </span>
                <span>${removeTaxonLink?lower_case?cap_first}</span>
            </a>
        </div>
        <div class="col-lg-6">
            <@input name="metadata.taxonomic.taxonID" help="i18n" i18nkey="datapackagemetadata.taxonomic.taxonId" requiredField=true />
        </div>
        <div class="col-lg-6">
            <@input name="metadata.taxonomic.taxonIDReference" help="i18n" i18nkey="datapackagemetadata.taxonomic.taxonIdReference" requiredField=true />
        </div>
        <div class="col-lg-6">
            <@input name="metadata.taxonomic.scientificName" help="i18n" i18nkey="datapackagemetadata.taxonomic.scientificName" requiredField=true />
        </div>
        <div class="col-lg-6">
            <@select name="metadata.taxonomic.taxonRank" help="i18n" includeEmpty=true options=taxonRanks i18nkey="datapackagemetadata.taxonomic.taxonRank" value=""/>
        </div>

        <div class="col-lg-6">
            <@input name="metadata.taxonomic.kingdom" help="i18n" i18nkey="datapackagemetadata.taxonomic.kingdom" />
        </div>
        <div class="col-lg-6">
            <@input name="metadata.taxonomic.phylum" help="i18n" i18nkey="datapackagemetadata.taxonomic.phylum" />
        </div>
        <div class="col-lg-6">
            <@input name="metadata.taxonomic.class" help="i18n" i18nkey="datapackagemetadata.taxonomic.class" />
        </div>
        <div class="col-lg-6">
            <@input name="metadata.taxonomic.order" help="i18n" i18nkey="datapackagemetadata.taxonomic.order" />
        </div>
        <div class="col-lg-6">
            <@input name="metadata.taxonomic.family" help="i18n" i18nkey="datapackagemetadata.taxonomic.family" />
        </div>
        <div class="col-lg-6">
            <@input name="metadata.taxonomic.genus" help="i18n" i18nkey="datapackagemetadata.taxonomic.genus" />
        </div>

        <div class="col-12">
            <span class="form-label">
                <a tabindex="0" role="button" class="popover-link" data-bs-toggle="popover" data-bs-trigger="focus" data-bs-html="true" data-bs-content="<@s.text name='datapackagemetadata.taxonomic.vernacularNames.help'/>" data-bs-original-title="" title="">
                    <i class="bi bi-info-circle text-gbif-primary px-1"></i>
                </a>
                <@s.text name="datapackagemetadata.taxonomic.vernacularNames"/>
            </span>
        </div>

        <div id="vernacularName-items" class="col-12 mt-0 vernacularName-items-wrapper"></div>

        <div class="addNew col-12 mt-2">
            <a id="plus-vernacularName" class="metadata-action-link" href="">
                <span>
                    <svg viewBox="0 0 24 24" style="fill: #4BA2CE;height: 1em;vertical-align: -0.125em !important;">
                        <path d="M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z"></path>
                    </svg>
                </span>
                <span>${addVernacularNameLink?lower_case?cap_first}</span>
            </a>
        </div>
    </div>

    <div id="baseItem-vernacularName" class="row g-3 mt-1 item" style="display: none;">
        <div class="columnLinks mt-2 d-flex justify-content-end">
            <a id="vernacularName-removeLink" href="" class="removeVernacularNameLink metadata-action-link">
            <span>
                <svg viewBox="0 0 24 24" style="fill: #4BA2CE;height: 1em;vertical-align: -0.125em !important;">
                    <path d="M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM8 9h8v10H8V9zm7.5-5-1-1h-5l-1 1H5v2h14V4h-3.5z"></path>
                </svg>
            </span>
                <span>${removeVernacularNameLink?lower_case?cap_first}</span>
            </a>
        </div>
        <div class="col-lg-6">
          <label for="metadata.taxonomic" class="form-label">
            Language code
          </label>
          <input class="form-control" type="text" id="vernacularNames-key" name="vernacularNames-key" value="">
        </div>
        <div class="col-lg-6">
          <label for="metadata.taxonomic" class="form-label">
            Vernacular name
          </label>
          <input class="form-control" type="text" id="vernacularNames-value" name="vernacularNames-value" value="">
        </div>
    </div>

    <#include "/WEB-INF/pages/inc/footer.ftl">
</#escape>