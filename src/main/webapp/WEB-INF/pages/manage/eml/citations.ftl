<#escape x as x?html>
    <#setting number_format="#####.##">
    <#include "/WEB-INF/pages/inc/header.ftl">
    <link rel="stylesheet" href="${baseURL}/styles/smaller-inputs.css">
    <title><@s.text name='manage.metadata.citations.title'/></title>
    <script>
        $(document).ready(function(){
            window.onLoad = populateCitationWithAutoGeneratedCitation();

            function populateCitationWithAutoGeneratedCitation() {
                var isAutoGenerated = ${resource.isCitationAutoGenerated()?c};
                if (isAutoGenerated) {
                    // auto-generated citation string
                    var autoCitation = "${action.resource.generateResourceCitation(resource.getNextVersion().toPlainString()?string, cfg.getResourceVersionUri(resource.getShortname(), resource.getNextVersion().toPlainString()?string))!}";
                    $("#eml\\.citation\\.citation").val(autoCitation);
                    $('#cit').attr("value", "true");
                    $("#generateOff").show();
                    $("#generateOn").hide();
                }
            }

            $("#generateOn").click(function(event) {
                event.preventDefault();
                // auto-generated citation string
                var autoCitation = "${action.resource.generateResourceCitation(resource.getNextVersion().toPlainString()?string, cfg.getResourceVersionUri(resource.getShortname(), resource.getNextVersion().toPlainString()?string))!}";
                $("#eml\\.citation\\.citation").val(autoCitation);
                $('#cit').attr("value", "true");
                $("#generateOff").show();
                $("#generateOn").hide();
            });
            $("#generateOff").click(function(event) {
                event.preventDefault();
                $("#eml\\.citation\\.citation").val("")
                $('#cit').attr("value", "false");
                $("#generateOn").show();
                $("#generateOff").hide();
            });

            $('#metadata-section').change(function () {
                var metadataSection = $('#metadata-section').find(':selected').val()
                $(location).attr('href', 'metadata-' + metadataSection + '.do?r=${resource.shortname!r!}');
            });

            // scroll to the error if present
            var invalidElements = $(".is-invalid");

            if (invalidElements !== undefined && invalidElements.length > 0) {
                var invalidElement = invalidElements.first();
                var pos = invalidElement.offset().top - 100;
                // scroll to the element
                $('body, html').animate({scrollTop: pos});
            }

            // reordering
            function initAndGetSortable(selector) {
                return sortable(selector, {
                    forcePlaceholderSize: true,
                    placeholderClass: 'border',
                    handle: '.handle'
                });
            }

            const sortable_keywords = initAndGetSortable('#items');

            sortable_keywords[0].addEventListener('sortupdate', changeInputNamesAfterDragging);
            sortable_keywords[0].addEventListener('drag', dragScroll);

            function dragScroll(e) {
                var cursor = e.pageY;
                var parentWindow = parent.window;
                var pixelsToTop = $(parentWindow).scrollTop();
                var screenHeight = $(parentWindow).height();

                if ((cursor - pixelsToTop) > screenHeight * 0.9) {
                    parentWindow.scrollBy(0, (screenHeight / 30));
                } else if ((cursor - pixelsToTop) < screenHeight * 0.1) {
                    parentWindow.scrollBy(0, -(screenHeight / 30));
                }
            }

            function changeInputNamesAfterDragging(e) {
                displayProcessing();
                var contactItems = $("#items div.item");

                contactItems.each(function (index) {
                    var elementId = $(this)[0].id;

                    $("div#" + elementId + " textarea[id$='citation']").attr("name", "eml.bibliographicCitationSet.bibliographicCitations[" + index + "].citation");
                    $("div#" + elementId + " input[id$='identifier']").attr("name", "eml.bibliographicCitationSet.bibliographicCitations[" + index + "].identifier");
                });

                hideProcessing();
            }

            makeSureResourceParameterIsPresentInURL('${resource.shortname}');
        });
    </script>
    <#include "/WEB-INF/pages/macros/metadata.ftl"/>
    <#assign currentMetadataPage = "citations"/>
    <#assign currentMenu="manage"/>
    <#include "/WEB-INF/pages/inc/menu.ftl">
    <#include "/WEB-INF/pages/macros/forms.ftl"/>

    <div class="container px-0">
        <#include "/WEB-INF/pages/inc/action_alerts.ftl">
    </div>

    <form class="needs-validation" action="metadata-${section}.do" method="post" novalidate>
        <div class="container-fluid bg-body border-bottom">
            <div class="container bg-body border rounded-2 mb-4">
                <div class="container my-3 p-3">
                    <div class="text-center fs-smaller">
                        <nav style="--bs-breadcrumb-divider: url(&#34;data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='8' height='8'%3E%3Cpath d='M2.5 0L1 1.5 3.5 4 1 6.5 2.5 8l4-4-4-4z' fill='currentColor'/%3E%3C/svg%3E&#34;);" aria-label="breadcrumb">
                            <ol class="breadcrumb justify-content-center mb-0">
                                <li class="breadcrumb-item"><a href="${baseURL}/manage/"><@s.text name="breadcrumb.manage"/></a></li>
                                <li class="breadcrumb-item"><a href="resource?r=${resource.shortname}"><@s.text name="breadcrumb.manage.overview"/></a></li>
                                <li class="breadcrumb-item active" aria-current="page"><@s.text name="breadcrumb.manage.overview.metadata"/></li>
                            </ol>
                        </nav>
                    </div>

                    <div class="text-center">
                        <h1 class="py-2 mb-0 text-gbif-header fs-2 fw-normal">
                            <@s.text name='manage.metadata.citations.title'/>
                        </h1>
                    </div>

                    <div class="text-center fs-smaller">
                        <a href="resource.do?r=${resource.shortname}" title="${resource.title!resource.shortname}">${resource.title!resource.shortname}</a>
                    </div>

                    <div class="text-center mt-2">
                        <@s.submit cssClass="button btn btn-sm btn-outline-gbif-primary top-button" name="save" key="button.save" />
                        <@s.submit cssClass="button btn btn-sm btn-outline-secondary top-button" name="cancel" key="button.back" />
                    </div>
                </div>
            </div>
        </div>

        <#include "metadata_section_select.ftl"/>

        <div class="container-fluid bg-body">
            <div class="container bd-layout main-content-container">
                <main class="bd-main">
                    <div class="bd-toc mt-4 mb-5 ps-3 mb-lg-5 text-muted">
                        <#include "eml_sidebar.ftl"/>
                    </div>

                    <div class="bd-content">
                        <div class="my-md-3 p-3">
                            <p class="mb-0">
                                <@s.text name='manage.metadata.citations.intro'/>
                            </p>

                            <div class="callout callout-info text-smaller">
                                <@s.text name='manage.metadata.citations.warning'/>
                            </div>

                            <!-- retrieve some link names one time -->
                            <#assign removeLink><@s.text name='manage.metadata.removethis'/> <@s.text name='manage.metadata.citations.item'/></#assign>
                            <#assign addLink><@s.text name='manage.metadata.addnew'/> <@s.text name='manage.metadata.citations.item'/></#assign>

                            <div class="row g-3">
                                <div class="mt-4 d-flex justify-content-end">
                                    <a id="generateOff" class="removeLink metadata-action-link custom-link" <#if resource.citationAutoGenerated?c != "true">style="display: none"</#if> href="">
                                        <span>
                                            <svg viewBox="0 0 24 24" class="link-icon link-icon-primary">
                                                <path d="M12 6v3l4-4-4-4v3c-4.42 0-8 3.58-8 8 0 1.57.46 3.03 1.24 4.26L6.7 14.8c-.45-.83-.7-1.79-.7-2.8 0-3.31 2.69-6 6-6zm6.76 1.74L17.3 9.2c.44.84.7 1.79.7 2.8 0 3.31-2.69 6-6 6v-3l-4 4 4 4v-3c4.42 0 8-3.58 8-8 0-1.57-.46-3.03-1.24-4.26z"></path>
                                            </svg>
                                        </span>
                                        <span><@s.text name='eml.citation.generate.turn.off'/></span>
                                    </a>
                                    <a id="generateOn" class="removeLink metadata-action-link custom-link" <#if resource.citationAutoGenerated?c == "true">style="display: none"</#if> href="">
                                        <span>
                                            <svg viewBox="0 0 24 24" class="link-icon link-icon-primary">
                                                <path d="M12 6v3l4-4-4-4v3c-4.42 0-8 3.58-8 8 0 1.57.46 3.03 1.24 4.26L6.7 14.8c-.45-.83-.7-1.79-.7-2.8 0-3.31 2.69-6 6-6zm6.76 1.74L17.3 9.2c.44.84.7 1.79.7 2.8 0 3.31-2.69 6-6 6v-3l-4 4 4 4v-3c4.42 0 8-3.58 8-8 0-1.57-.46-3.03-1.24-4.26z"></path>
                                            </svg>
                                        </span>
                                        <span>
                                            <@s.text name='eml.citation.generate.turn.on'/>
                                        </span>
                                    </a>
                                    <input id="cit" name="resource.citationAutoGenerated" type=hidden value="${resource.citationAutoGenerated?c}"/>
                                </div>
                                <@text name="eml.citation.citation" help="i18n" requiredField=true />
                                <#if resource.doi?? && doiReservedOrAssigned>
                                    <@input name="eml.citation.identifier" help="i18n" disabled=true value="${resource.doi.getUrl()!}"/>
                                <#else>
                                    <@input name="eml.citation.identifier" help="i18n"/>
                                </#if>
                            </div>

                        </div>

                        <div class="my-md-3 p-3">
                            <div class="listBlock">
                                <@textinline name="manage.metadata.citations.bibliography" help="i18n"/>

                                <div id="bibliographic-citations-info" class="callout callout-info text-smaller">
                                    <@s.text name="manage.metadata.citations.warning.doNotNumber"/>
                                </div>

                                <div id="items">
                                    <#list eml.bibliographicCitationSet.bibliographicCitations as item>
                                        <div id="item-${item_index}" class="item row g-3 border-bottom pb-3 mt-1">
                                            <div class="handle d-flex mt-2 justify-content-end">
                                                <a id="removeLink-${item_index}" class="removeLink metadata-action-link custom-link" href="">
                                                    <span>
                                                        <svg viewBox="0 0 24 24" class="link-icon link-icon-danger">
                                                            <path d="M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z"></path>
                                                        </svg>
                                                    </span>
                                                    <span>${removeLink?lower_case?cap_first}</span>
                                                </a>
                                            </div>
                                            <@text name="eml.bibliographicCitationSet.bibliographicCitations[${item_index}].citation" help="i18n" i18nkey="eml.bibliographicCitationSet.bibliographicCitations.citation" size=40 requiredField=true />
                                            <@input name="eml.bibliographicCitationSet.bibliographicCitations[${item_index}].identifier" help="i18n" i18nkey="eml.bibliographicCitationSet.bibliographicCitations.identifier" />
                                        </div>
                                    </#list>
                                </div>
                            </div>

                            <div class="addNew col-12 mt-1">
                                <a id="plus" href="" class="metadata-action-link custom-link">
                                    <span>
                                        <svg viewBox="0 0 24 24" class="link-icon link-icon-primary">
                                            <path d="M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z"></path>
                                        </svg>
                                    </span>
                                    <span>${addLink?lower_case?cap_first}</span>
                                </a>
                            </div>

                            <!-- internal parameter -->
                            <input name="r" type="hidden" value="${resource.shortname}" />

                            <div id="baseItem" class="item row g-3 border-bottom pb-3 mt-1" style="display:none;">
                                <div class="handle mt-2 d-flex justify-content-end">
                                    <a id="removeLink" class="removeLink metadata-action-link custom-link" href="">
                                        <span>
                                            <svg viewBox="0 0 24 24" class="link-icon link-icon-danger">
                                                <path d="M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z"></path>
                                            </svg>
                                        </span>
                                        <span><@s.text name='manage.metadata.removethis'/> <@s.text name='manage.metadata.citations.item'/></span>
                                    </a>
                                </div>
                                <@text name="citation" help="i18n" i18nkey="eml.bibliographicCitationSet.bibliographicCitations.citation"  value="" size=40 requiredField=true />
                                <@input name="identifier" help="i18n" i18nkey="eml.bibliographicCitationSet.bibliographicCitations.identifier" />
                            </div>
                        </div>
                    </div>
                </main>
            </div>
        </div>
    </form>

    <#include "/WEB-INF/pages/inc/footer.ftl">
</#escape>
