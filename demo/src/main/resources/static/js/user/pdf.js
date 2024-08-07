document.addEventListener('DOMContentLoaded', () => {
    function getQueryParam(param) {
        const urlParams = new URLSearchParams(window.location.search);
        return urlParams.get(param);
    }

    const fileId = getQueryParam('fileId');
    if (!fileId) {
        alert('File ID not provided!');
        throw new Error('File ID not provided');
    }

    const API_BASE_URL = "8.130.130.240:8088";
    const url = `http://${API_BASE_URL}/downloadfiles/${encodeURIComponent(fileId)}`;

    pdfjsLib.GlobalWorkerOptions.workerSrc = 'https://cdnjs.cloudflare.com/ajax/libs/pdf.js/2.11.338/pdf.worker.min.js';

    const pdfViewer = document.getElementById('pdfViewer');
    const canvasCountElem = document.getElementById('canvasCount');
    const pageNumberInput = document.getElementById('pageNumberInput');
    const searchButton = document.getElementById('searchButton');
    const searchTextInput = document.getElementById('searchText');
    let pdfDoc = null;
    let searchText = '';

    const renderPage = async (pageNum) => {
        const page = await pdfDoc.getPage(pageNum);
        const viewport = page.getViewport({ scale: 1.5 });

        const canvas = document.createElement('canvas');
        const context = canvas.getContext('2d');
        canvas.height = viewport.height;
        canvas.width = viewport.width;

        const renderContext = {
            canvasContext: context,
            viewport: viewport
        };

        await page.render(renderContext).promise;

        // Add page number at the bottom
        context.font = '16px Arial';
        context.fillStyle = 'black';
        context.textAlign = 'center';
        context.fillText(`Page ${pageNum}`, canvas.width / 2, canvas.height - 10);

        pdfViewer.appendChild(canvas);

        if (searchText) {
            highlightText(page, viewport, searchText, context);
        }
    };

    const renderAllPages = async () => {
        pdfViewer.innerHTML = '';
        for (let pageNum = 1; pageNum <= pdfDoc.numPages; pageNum++) {
            await renderPage(pageNum);
        }
        canvasCountElem.textContent = pdfDoc.numPages;
    };

    const highlightText = async (page, viewport, searchText, context) => {
        const textContent = await page.getTextContent();
        const matches = [];
        textContent.items.forEach(item => {
            if (item.str.includes(searchText)) {
                const textItem = {
                    transform: item.transform,
                    str: item.str
                };
                matches.push(textItem);
            }
        });

        matches.forEach(item => {
            const tx = pdfjsLib.Util.transform(viewport.transform, item.transform);
            const minX = Math.min(tx[4], tx[4] + tx[0] * item.str.length);
            const maxX = Math.max(tx[4], tx[4] + tx[0] * item.str.length);
            const minY = Math.min(tx[5], tx[5] + tx[3]);
            const maxY = Math.max(tx[5], tx[5] + tx[3]);

            context.globalAlpha = 0.4;
            context.fillStyle = 'yellow';
            context.fillRect(minX, viewport.height - maxY, maxX - minX, maxY - minY);
        });
    };

    pdfjsLib.getDocument(url).promise.then(pdf => {
        pdfDoc = pdf;
        renderAllPages();
    }).catch(error => {
        console.error('Error loading PDF:', error);
        alert('Error loading PDF: ' + error.message);
    });

    document.getElementById('goToPage').addEventListener('click', () => {
        const pageNumber = parseInt(pageNumberInput.value);
        if (pageNumber > 0 && pageNumber <= pdfDoc.numPages) {
            const canvas = pdfViewer.querySelectorAll('canvas')[pageNumber - 1];
            if (canvas) {
                canvas.scrollIntoView();
            }
        }
    });

    const searchInDocument = async (searchText) => {
        for (let pageNum = 1; pageNum <= pdfDoc.numPages; pageNum++) {
            const page = await pdfDoc.getPage(pageNum);
            const textContent = await page.getTextContent();
            const strings = textContent.items.map(item => item.str);
            if (strings.some(str => str.includes(searchText))) {
                pdfViewer.innerHTML = '';
                for (let num = pageNum; num <= pdfDoc.numPages; num++) {
                    await renderPage(num);
                }
                canvasCountElem.textContent = pdfDoc.numPages;
                return;
            }
        }
        alert(`"${searchText}" not found in the document.`);
    };

    searchButton.addEventListener('click', () => {
        searchText = searchTextInput.value;
        if (searchText) {
            searchInDocument(searchText);
        }
    });

    // Disable right-click context menu
    document.addEventListener('contextmenu', event => event.preventDefault());

    // Disable download, print, and share shortcuts
    document.addEventListener('keydown', function(event) {
        const forbiddenCombos = [
            { key: 'p', ctrl: true },
            { key: 's', ctrl: true },
            { key: 's', ctrl: true, shift: true },
            { key: 'p', ctrl: true, shift: true },
            { key: 'i', ctrl: true, shift: true },
            { key: 'u', ctrl: true },
            { key: 'j', ctrl: true, shift: true },
        ];

        const isForbidden = forbiddenCombos.some(combo =>
            event.key.toLowerCase() === combo.key &&
            (!combo.ctrl || event.ctrlKey || event.metaKey) &&
            (!combo.shift || event.shiftKey)
        );

        if (isForbidden) {
            event.preventDefault();
            alert('This function is disabled');
        }
    });
});
