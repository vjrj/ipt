// Function to convert HTML to DocBook
function convertToDocBook(html) {
    // Replace <div> with <section>
    html = html.replace(/<div>/g, '<section>').replace(/<\/div>/g, '</section>');

    // Replace <h> with <title>
    html = html
        .replace(/<h1>/g, '<title>').replace(/<\/h1>/g, '</title>')
        .replace(/<h2>/g, '<title>').replace(/<\/h2>/g, '</title>')
        .replace(/<h3>/g, '<title>').replace(/<\/h3>/g, '</title>')
        .replace(/<h4>/g, '<title>').replace(/<\/h4>/g, '</title>')
        .replace(/<h5>/g, '<title>').replace(/<\/h5>/g, '</title>');

    // Replace <ul> with <itemizedlist>
    // Replace <ol> with <orderedlist>
    // Replace <li> with <listitem>
    // Also, wrap into <para> where needed
    html = html
        .replace(/<ul>/g, '<para><itemizedlist>').replace(/<\/ul>/g, '</itemizedlist></para>')
        .replace(/<ol>/g, '<para><orderedlist>').replace(/<\/ol>/g, '</orderedlist></para>')
        .replace(/<li>/g, '<listitem><para>').replace(/<\/li>/g, '</para></listitem>');

    // Replace <p> with <para>
    html = html.replace(/<p>/g, '<para>').replace(/<\/p>/g, '</para>');

    // Replace <b> with <emphasis>
    html = html.replace(/<b>/g, '<emphasis>').replace(/<\/b>/g, '</emphasis>');

    // Replace <sub> with <subscript> and <sup> with <superscript>
    html = html.replace(/<sub>/g, '<subscript>').replace(/<\/sub>/g, '</subscript>');
    html = html.replace(/<sup>/g, '<superscript>').replace(/<\/sup>/g, '</superscript>');

    // Replace <pre> with <literal>
    html = html.replace(/<pre>/g, '<literal>').replace(/<\/pre>/g, '</literal>');

    // Remove <br>
    html = html.replace(/<br>/g, '').replace(/<\/br>/g, '');

    // Replace <a href="...">...</a> with <ulink url="..."><citetitle>...</citetitle></ulink>
    html = html.replace(/<a href="([^"]+)">([^<]+)<\/a>/g, '<ulink url="$1"><citetitle>$2</citetitle></ulink>');

    return html;
}

// Function to convert DocBook to HTML
function convertToHtml(docBook) {
    // Decode HTML entities
    docBook = $('<textarea />').html(docBook).text();

    // Replace <section> with <div>
    docBook = docBook.replace(/<section>/g, '<div>').replace(/<\/section>/g, '</div>');

    // Replace <title> with <h1>
    docBook = docBook.replace(/<title>/g, '<h1>').replace(/<\/title>/g, '</h1>');

    // Replace <itemizedlist> with <ul>
    // Replace <orderedlist> with <ol>
    // Replace <listitem> with <li>
    // Also, unwrap <para> where needed
    docBook = docBook
        .replace(/<para><itemizedlist>/g, '<ul>').replace(/<\/itemizedlist><\/para>/g, '</ul>')
        .replace(/<para><orderedlist>/g, '<ol>').replace(/<\/orderedlist><\/para>/g, '</ol>')
        .replace(/<listitem><para>/g, '<li>').replace(/<\/para><\/listitem>/g, '</li>');

    // Replace <para> with <p>
    docBook = docBook.replace(/<para>/g, '<p>').replace(/<\/para>/g, '</p>');

    // Replace <emphasis> with <b>
    docBook = docBook.replace(/<emphasis>/g, '<b>').replace(/<\/emphasis>/g, '</b>');

    // Replace <subscript> with <sub> and <superscript> with <sup>
    docBook = docBook.replace(/<subscript>/g, '<sub>').replace(/<\/subscript>/g, '</sub>');
    docBook = docBook.replace(/<superscript>/g, '<sup>').replace(/<\/superscript>/g, '</sup>');

    // Replace <literal> with <pre>
    docBook = docBook.replace(/<literal>/g, '<pre>').replace(/<\/literal>/g, '</pre>');

    // Replace <ulink url="..."><citetitle>...</citetitle></ulink> with <a href="...">...</a>
    docBook = docBook.replace(/<ulink url="([^"]+)"><citetitle>([^<]+)<\/citetitle><\/ulink>/g, '<a href="$1">$2</a>');

    return docBook;
}

// Function to validate HTML (it must contain only allowed tags)
function validateHTML(html) {
    // Define allowed tags
    const allowedTags = [
        'div',
        'h1', 'h2', 'h3', 'h4', 'h5',
        'ul', 'ol', 'li',
        'p', 'b', 'sub', 'sup', 'pre', 'a'
    ];

    // Match all HTML tags in the string
    const regex = /<\/?([a-z][a-z0-9]*)\b[^>]*>/gi;
    let match;

    // Loop through all found tags
    while ((match = regex.exec(html)) !== null) {
        // Extract the tag name from the match
        const tagName = match[1].toLowerCase();

        // Check if the tag is not in the allowed list
        if (!allowedTags.includes(tagName)) {
            return { isValid: false, tag: match[0] }; // Forbidden tag found
        }
    }

    return { isValid: true }; // No forbidden tags found
}
