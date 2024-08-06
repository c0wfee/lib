document.addEventListener('DOMContentLoaded', function () {
    function updateSearchField() {
        var searchType = document.getElementById('searchType').value;
        var searchTitleGroup = document.getElementById('searchTitleGroup');
        var searchIsbnGroup = document.getElementById('searchIsbnGroup');
        var searchAlternativeTitleGroup = document.getElementById('searchAlternativeTitleGroup');
        var searchAuthorGroup = document.getElementById('searchAuthorGroup');
        var searchAllkindsGroup = document.getElementById('searchAllkindsGroup');

        // Hide all search fields
        searchTitleGroup.style.display = 'none';
        searchIsbnGroup.style.display = 'none';
        searchAlternativeTitleGroup.style.display = 'none';
        searchAuthorGroup.style.display = 'none';
        searchAllkindsGroup.style.display = 'none';

        // Show the selected search field
        if (searchType === 'title') {
            searchTitleGroup.style.display = 'block';
        } else if (searchType === 'isbn') {
            searchIsbnGroup.style.display = 'block';
        } else if (searchType === 'alternativeTitle') {
            searchAlternativeTitleGroup.style.display = 'block';
        } else if (searchType === 'author') {
            searchAuthorGroup.style.display = 'block';
        } else if (searchType === 'allkinds') {
            searchAllkindsGroup.style.display = 'block';
        }
    }

    document.getElementById('searchForm').addEventListener('submit', function (event) {
        var searchType = document.getElementById('searchType').value;
        var searchValueInput = document.getElementById('searchValue');
        var searchTitle = document.getElementById('searchTitle');
        var searchIsbn = document.getElementById('searchIsbn');
        var searchAlternativeTitle = document.getElementById('searchAlternativeTitle');
        var searchAuthor = document.getElementById('searchAuthor');
        var searchAllkinds = document.getElementById('searchAllkinds');

        // Set the value for searchValue based on the selected searchType
        if (searchType === 'title') {
            searchValueInput.value = searchTitle.value;
        } else if (searchType === 'isbn') {
            searchValueInput.value = searchIsbn.value;
        } else if (searchType === 'alternativeTitle') {
            searchValueInput.value = searchAlternativeTitle.value;
        } else if (searchType === 'author') {
            searchValueInput.value = searchAuthor.value;
        } else if (searchType === 'allkinds') {
            searchValueInput.value = searchAllkinds.value;
        }
    });

    // Initialize the form to show the correct field based on the current search type
    updateSearchField();

    function showLoanEditModal(button) {
        var id = button.getAttribute('data-id'); // 获取数据ID
        var type = button.getAttribute('data-type'); // 获取数据类型
        var value = button.getAttribute('data-value'); // 获取当前值

        $('#loanEditId').val(id); // 将ID设置到隐藏字段
        $('#loanEditType').val(type); // 将类型设置到隐藏字段

        // Set the current value in the input field
        $('#loanEditField').val(value); // 将当前值设置到输入字段

        $('#loanEditModal').modal('show'); // 显示模态框
    }

    function showEditModal(button) {
        var id = button.getAttribute('data-id');
        var type = button.getAttribute('data-type');
        var value = button.getAttribute('data-value');

        $('#editId').val(id);
        $('#editType').val(type);

        var select = $('#editField');
        select.empty();  // Clear existing options

        if (type === 'status') {
            select.append('<option value="Published">Published</option>');
            select.append('<option value="Unpublished">Unpublished</option>');
        } else {
            select.append('<option value="Enable">Enable</option>');
            select.append('<option value="Disable">Disable</option>');
        }

        select.val(value);  // Set the current value
        $('#editModal').modal('show');
    }
});
