[#ftl output_format="HTML"]
[#include "/WEB-INF/pages/inc/header.ftl"/]
<title>[@s.text name="404.title"/]</title>
[#include "/WEB-INF/pages/inc/menu.ftl"/]

<div class="container px-0">
    [#include "/WEB-INF/pages/inc/action_alerts_warnings.ftl"]
    [#include "/WEB-INF/pages/inc/action_alerts_errors.ftl"]
</div>

<div class="container-fluid bg-body border-bottom">
    <div class="container bg-body border rounded-2 mb-4">
        <div class="container my-3 p-3">
            <div class="text-center fs-smaller">
                [@s.text name="basic.error"/]
            </div>

            <div class="text-center">
                <h1 class="pb-2 mb-0 pt-2 text-gbif-header fs-2 fw-normal">
                    [@s.text name="404.title"/]
                </h1>
            </div>
        </div>
    </div>
</div>

<main class="container main-content-container">
    <div class="my-3 p-3">
        <p class="text-center">[@s.text name="404.body"/]</p>
    </div>
</main>

[#include "/WEB-INF/pages/inc/footer.ftl"/]
