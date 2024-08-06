$(document).ready(function () {
    $('#uploadMarcModal').on('show.bs.modal', function (event) {
        var button = $(event.relatedTarget);
        var folderId = button.data('id');
        var modal = $(this);
        modal.find('#uploadMarcFolderId1').val(folderId);
    });

    $('#uploadEXCELModal').on('show.bs.modal', function (event) {
        var button = $(event.relatedTarget);
        var folderId = button.data('id');
        var modal = $(this);
        modal.find('#uploadEXCELFolderId').val(folderId);
    });

    $('#uploadEPUBModal').on('show.bs.modal', function (event) {
        var button = $(event.relatedTarget);
        var folderId = button.data('id');
        var modal = $(this);
        modal.find('#uploadEPUBFolderId').val(folderId);
    });

    $('#renameFolderModal').on('show.bs.modal', function (event) {
        var button = $(event.relatedTarget);
        var folderId = button.data('id');
        var folderName = button.data('name');
        var alternateNames = button.data('alternate-names');
        var description = button.data('description');
        var type = button.data('type');

        var modal = $(this);
        modal.find('#renameFolderId').val(folderId);
        modal.find('#newFolderName').val(folderName);
        modal.find('#newAlternateNames').val(alternateNames);
        modal.find('#newType').val(type);
        modal.find('#newDescription').val(description);
    });

    $('#uploadPdfModal').on('show.bs.modal', function (event) {
        var button = $(event.relatedTarget);
        var folderId = button.data('id');
        var modal = $(this);
        modal.find('#uploadPdfFolderId1').val(folderId);
    });

    // Handle form submission
    $('#addFolderForm').on('submit', function (event) {
        event.preventDefault();

        var form = $(this);
        var formData = new FormData(this);

        // Remove file inputs from FormData if they are empty
        if (!$('#marcFile')[0].files.length) {
            formData.delete('file');
        }
        if (!$('#excelFile1')[0].files.length) {
            formData.delete('excel');
        }
        if (!$('#pdfFiles')[0].files.length) {
            formData.delete('files');
        }
        if (!$('#epubFile1')[0].files.length) {
            formData.delete('epub');
        }
        console.log(formData);
        $.ajax({
            url: form.attr('action'),
            type: form.attr('method'),
            data: formData,
            processData: false,
            contentType: false,
            success: function (response) {
                window.location.href = "/resourcesLib";
            },
            error: function (error) {
                alert("Error submitting the form");
                console.error("Error:", error);
            }
        });
    });
});

function performAction(action, folderId) {
    let urlMap = {
        'hideBook': '/hideBook',
        'showBook': '/showBook',
        'cancelView': '/cancelView',
        'ableView': '/AbleView',
        'cancelDownload': '/cancelDownload',
        'ableDownload': '/ableDownload',
        'cancelBorrow': '/cancelBorrow',
        'ableBorrow': '/ableBorrow'
    };

    let url = urlMap[action];
    if (!url) {
        console.error('Invalid action:', action);
        return;
    }

    let data = new URLSearchParams();
    data.append('folderId', folderId);

    if (action === 'ableBorrow') {
        let period = prompt("Please enter the borrow period (days):", "30");
        if (period != null) {
            data.append('borrow_period', period);
        } else {
            return; // User cancelled the prompt
        }
    }

    fetch(url, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded',
        },
        body: data
    })
        .then(response => response.json())
        .then(data => {
            console.log('Success:', data);
            location.reload();
            alert('Operation successful');
        })
        .catch((error) => {
            console.error('Error:', error);
            alert('Operation failed');
        });
}

function handleBorrowChange(selectElement) {
    if (selectElement.value === 'Enable') {
        let period = prompt("Please enter the borrow period (days):", "30");
        if (period != null && !isNaN(period)) {
            document.getElementById('borrowHidden').value = period;
            selectElement.options[selectElement.selectedIndex].text = 'Enable (' + period + ' days)';
        } else {
            selectElement.value = ""; // Reset to default if invalid input
            document.getElementById('borrowHidden').value = "";
            alert("Please enter a valid number.");
        }
    } else {
        document.getElementById('borrowHidden').value = selectElement.value;
    }
}

function updateBorrowField() {
    const borrowSelect = document.getElementById('borrow');
    const borrowHidden = document.getElementById('borrowHidden');
    if (borrowSelect.value === 'Enable') {
        borrowHidden.name = 'borrow'; // Change the name to ensure it's sent as 'borrow'
    } else {
        borrowHidden.name = ''; // Reset name to avoid sending it
    }
}

function handleBorrowChange1(selectElement) {
    if (selectElement.value === 'Enable') {
        let period = prompt("Please enter the borrow period (days):", "30");
        if (period != null && !isNaN(period)) {
            document.getElementById('borrowHidden1').value = period;
            selectElement.options[selectElement.selectedIndex].text = 'Enable (' + period + ' days)';
        } else {
            selectElement.value = ""; // Reset to default if invalid input
            document.getElementById('borrowHidden1').value = "";
            alert("Please enter a valid number.");
        }
    } else {
        document.getElementById('borrowHidden1').value = selectElement.value;
    }
}

function updateBorrowField1() {
    const borrowSelect1 = document.getElementById('borrow1');
    const borrowHidden1 = document.getElementById('borrowHidden1');
    if (borrowSelect1.value === 'Enable') {
        borrowHidden1.name = 'borrow'; // Change the name to ensure it's sent as 'borrow'
    } else {
        borrowHidden1.name = ''; // Reset name to avoid sending it
    }
}

function handleBorrowChange2(selectElement) {
    if (selectElement.value === 'Enable') {
        let period = prompt("Please enter the borrow period (days):", "30");
        if (period != null && !isNaN(period)) {
            document.getElementById('borrowHidden2').value = period;
            selectElement.options[selectElement.selectedIndex].text = 'Enable (' + period + ' days)';
        } else {
            selectElement.value = ""; // Reset to default if invalid input
            document.getElementById('borrowHidden2').value = "";
            alert("Please enter a valid number.");
        }
    } else {
        document.getElementById('borrowHidden2').value = selectElement.value;
    }
}

function updateBorrowField2() {
    const borrowSelect2 = document.getElementById('borrow2');
    const borrowHidden2 = document.getElementById('borrowHidden2');
    if (borrowSelect2.value === 'Enable') {
        borrowHidden2.name = 'borrow'; // Change the name to ensure it's sent as 'borrow'
    } else {
        borrowHidden2.name = ''; // Reset name to avoid sending it
    }
}
