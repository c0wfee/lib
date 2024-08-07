
async function fetchBooks(username) {
    try {
        const response = await fetch(`/api/borrows/${username}`);
        if (!response.ok) {
            throw new Error('Network response was not ok');
        }
        const books = await response.json();
        displayBooks(books);
    } catch (error) {
        console.error('Error fetching books:', error);
    }
}

function displayBooks(books) {
    const container = document.getElementById('books-container');
    container.innerHTML = ''; // Clear the existing content
    if (books.length === 0) {
        container.innerHTML = '<p class="text-center">No books borrowed.</p>';
        return;
    }

    books.forEach(book => {
        const card = document.createElement('div');
        card.className = 'card';

        const cardHeader = document.createElement('div');
        cardHeader.className = 'card-header';
        cardHeader.textContent = book.bookTitle;

        const cardBody = document.createElement('div');
        cardBody.className = 'card-body';

        const bookId = document.createElement('p');
        bookId.className = 'card-text';
        bookId.textContent = 'Book ID: ' + book.bookId;

        const loanStartTime = document.createElement('p');
        loanStartTime.className = 'card-text';
        loanStartTime.textContent = 'Loan Start Time: ' + book.loanStartTime;

        const loanEndTime = document.createElement('p');
        loanEndTime.className = 'card-text';
        loanEndTime.textContent = 'Loan End Time: ' + book.loanEndTime;

        const viewButton = document.createElement('button');
        viewButton.className = 'btn btn-primary view-button';
        viewButton.textContent = 'View';
        viewButton.addEventListener('click', () => {
            viewBook(book.bookId);
        });

        const returnButton = document.createElement('button');
        returnButton.className = 'btn btn-primary view-button';
        returnButton.textContent = 'Return';
        returnButton.addEventListener('click', () => {
            returnBook(book.bookId);
        });

        cardBody.appendChild(bookId);
        cardBody.appendChild(loanStartTime);
        cardBody.appendChild(loanEndTime);
        cardBody.appendChild(viewButton);
        cardBody.appendChild(returnButton);
        card.appendChild(cardHeader);
        card.appendChild(cardBody);
        container.appendChild(card);
    });
}

function viewBook(bookId) {
    console.log('View book with ID:', bookId);
    const display = null; // Adjust this based on your needs
    if (display == null) {
        window.location.href = `/pdf?fileId=${encodeURIComponent(bookId)}`;
    } else {
        fetch(`http://8.130.130.240:8088/downloadfiles/${encodeURIComponent(bookId)}`, {responseType: 'blob'})
            .then(response => {
                if (!response.ok) {
                    throw new Error('Network response was not ok');
                }
                return response.blob();
            })
            .then(blob => {
                const url = window.URL.createObjectURL(blob);
                window.open(url);
            })
            .catch(error => {
                console.error('Error downloading book:', error);
            });
    }
}

function returnBook(bookId) {
    let data = new URLSearchParams();
    data.append('bookId', bookId);
    fetch("/returnBook", {
        method: 'POST',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
        },
        body: data
    })
        .then(response => response.json())
        .then(data => {
            console.log('Success:', data);
            alert('Operation successful');
        })
        .catch((error) => {
            console.error('Error:', error);
            alert('Operation failed');
        });

}

document.addEventListener('DOMContentLoaded', () => {
    const username = 'admin'; // Replace this with the actual username
    fetchBooks(username);
});
