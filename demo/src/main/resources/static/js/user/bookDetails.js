import { BASE_URL } from '../config.js';

document.addEventListener('DOMContentLoaded', function() {
    const bookId = document.getElementById('fileId').value;
    console.log('Book ID:', bookId);
    if (bookId !== 'default_id') {
        fetch(`${BASE_URL}/book/${bookId}`)
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

                const loanPeriodElement = document.getElementById('loan-period-container');
                const borrowButton = document.getElementById('borrow-button');
                const loanInfoElement = document.getElementById('loan-info');

                if (data.borrowPeriod > 0) {
                    document.getElementById('loan-period').textContent = `${data.borrowPeriod} Days`;

                    if (data.loanLabel === 'Borrowed') {
                        borrowButton.disabled = true;
                        borrowButton.textContent = 'Borrowed';
                        borrowButton.classList.add('disabled');
                    } else {
                        borrowButton.textContent = 'Borrow';
                        borrowButton.disabled = false;
                        borrowButton.onclick = function() {
                            borrowBook(bookId);
                        };
                    }

                    // Fetch loan info if the book is borrowed
                    if (data.loanLabel === 'Borrowed') {
                        fetch(`${BASE_URL}/borrow/${bookId}`)
                            .then(response => response.json())
                            .then(borrowData => {
                                if (borrowData.loanEndTime) {
                                    loanInfoElement.textContent = `On Loan, unavailable until ${borrowData.loanEndTime}`;
                                    loanInfoElement.style.display = 'block';
                                } else {
                                    loanInfoElement.style.display = 'none';
                                }
                            })
                            .catch(error => {
                                console.error('Error fetching borrow details:', error);
                                loanInfoElement.style.display = 'none';
                            });
                    } else {
                        loanInfoElement.style.display = 'none';
                    }
                } else {
                    // Hide borrow button and loan period info if borrowPeriod is 0
                    loanPeriodElement.style.display = 'none';
                    borrowButton.style.display = 'none';
                    loanInfoElement.style.display = 'none';
                }
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

async function borrowBook(id) {
    try {
        let periodUrl = `/getBookPeriod?bookID=${id}`;
        let periodResponse = await fetch(periodUrl, {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json',
            }
        });

        if (!periodResponse.ok) {
            throw new Error('Failed to fetch borrow period');
        }

        let borrowPeriodText = await periodResponse.text();
        let borrowPeriod = parseInt(borrowPeriodText, 10);

        let data = new URLSearchParams();
        data.append('bookID', id);
        data.append('borrow_period', borrowPeriod);

        let borrowResponse = await fetch("/borrowBook", {
            method: 'POST',
            headers: {
                'Content-Type': 'application/x-www-form-urlencoded',
            },
            body: data
        });

        if (!borrowResponse.ok) {
            throw new Error('Failed to borrow book');
        }

        let borrowData = await borrowResponse.json();
        console.log('Success:', borrowData);
        alert('Operation successful');

        // Refresh the page to reflect the borrow action
        window.location.reload();
    } catch (error) {
        console.error('Error:', error);
        alert('Operation failed');
    }
}
