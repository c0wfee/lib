async function fetchBooks() {
    try {
        const response = await fetch('/api/borrows');
        const books = await response.json();
        displayBooks(books);
    } catch (error) {
        console.error('Error fetching books:', error);
    }
}

function displayBooks(books) {
    const tableBody = document.getElementById('books-table-body');
    tableBody.innerHTML = ''; // Clear the existing content
    books.forEach(book => {
        const row = document.createElement('tr');

        const borrowIdCell = document.createElement('td');
        borrowIdCell.textContent = book.borrowId;
        row.appendChild(borrowIdCell);

        const usernameCell = document.createElement('td');
        usernameCell.textContent = book.username;
        row.appendChild(usernameCell);

        const bookIdCell = document.createElement('td');
        bookIdCell.textContent = book.bookId;
        row.appendChild(bookIdCell);

        const loanStartTimeCell = document.createElement('td');
        loanStartTimeCell.textContent = book.loanStartTime;
        row.appendChild(loanStartTimeCell);

        const loanEndTimeCell = document.createElement('td');
        loanEndTimeCell.textContent = book.loanEndTime;
        row.appendChild(loanEndTimeCell);

        const bookTitleCell = document.createElement('td');
        bookTitleCell.textContent = book.bookTitle;
        row.appendChild(bookTitleCell);

        tableBody.appendChild(row);
    });
}

document.addEventListener('DOMContentLoaded', fetchBooks);
