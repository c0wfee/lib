document.addEventListener('DOMContentLoaded', function() {
    const bookId = document.getElementById('fileId').value;
    console.log('Book ID:', bookId);
    if (bookId !== 'default_id') {
        fetch(`http://8.130.130.240:8088/book/${bookId}`)
            .then(response => {
                if (!response.ok) {
                    throw new Error(`Network response was not ok: ${response.statusText}`);
                }
                return response.json();
            })
            .then(data => {
                document.getElementById('source-image').src = '../static/images/book.png';
                updateField('book-title', data.title);
                updateField('authors', data.authors, 'authors-container');
                updateField('editors', data.editors, 'editors-container');
                updateField('series', data.series, 'series-container');
                updateField('isbn', data.isbn, 'isbn-container');
                updateField('publisher', data.publisher, 'publisher-container');
                updateField('published', data.published, 'published-container');
                updateField('edition', data.edition, 'edition-container');
                updateField('copyright-declaration', data.copyright_declaration, 'copyright-declaration-container');
                updateField('pages', data.pages, 'pages-container');
                updateField('subjects', data.subjects || 'N/A', 'subjects-container');
                updateField('description', data.description || '', 'description-container');
                updateField('loan-period', data.loan_label, 'loan-period-container');
            })
            .catch(error => {
                console.error('Error fetching book details:', error);
                alert(`Error fetching book details: ${error.message}`);
            });
    } else {
        console.error('No book ID provided in URL');
    }
});

function updateField(elementId, value, containerId = null) {
    const element = document.getElementById(elementId);
    if (value) {
        element.textContent = value;
    } else if (containerId) {
        document.getElementById(containerId).style.display = 'none';
    }
}

function goBack() {
    window.location.href = 'fileList.html';
}
