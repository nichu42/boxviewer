(function() {
    const params = new URLSearchParams(window.location.search);
    const boxId = params.get('id');
    const heading = document.querySelector('h1');
    const statusText = document.getElementById('status-text');
    const actionBtn = document.getElementById('action-btn');
    const spinner = document.getElementById('loading-spinner');

    if (boxId) {
        // Validate if it is a 24-character hexadecimal MongoDB ObjectId
        if (/^[0-9a-fA-F]{24}$/.test(boxId)) {
            spinner.style.display = "block";
            heading.textContent = "Opening senseBox Station";
            statusText.textContent = "We are attempting to open this environmental sensor station directly inside the BoxViewer app...";
            actionBtn.textContent = "Open in BoxViewer";

            const deepLink = `boxviewer://box/${boxId}`;
            actionBtn.href = deepLink;

            // Try to redirect immediately
            window.location.href = deepLink;

            // Fallback for browsers that don't redirect automatically
            setTimeout(() => {
                spinner.style.display = "none";
                statusText.textContent = "If the station did not open automatically, tap the button below to launch the app. If you don't have BoxViewer installed, you can download the open-source client from Codeberg.";
                actionBtn.textContent = "Open in App";

                const installContainer = document.createElement('div');
                installContainer.innerHTML = '<a href="https://codeberg.org/nichu42/BoxViewer" class="install-link">Get BoxViewer on Codeberg &rarr;</a>';
                actionBtn.parentNode.insertBefore(installContainer, actionBtn.nextSibling);
            }, 2500);
        } else {
            heading.textContent = "Invalid Station Link";
            statusText.textContent = "The senseBox ID provided in this URL is invalid or malformed. Please verify that you have copied the correct sharing link from your device.";
            actionBtn.textContent = "Visit Codeberg Project";
            actionBtn.href = "https://codeberg.org/nichu42/BoxViewer";
        }
    } else {
        heading.textContent = "BoxViewer for openSenseMap";
        statusText.textContent = "BoxViewer is a beautiful, tracking-free Android companion client for the openSenseMap community. This sharing page helps you open community weather stations and DIY senseBoxes directly in the app.";
        actionBtn.textContent = "Get BoxViewer on Codeberg";
        actionBtn.href = "https://codeberg.org/nichu42/BoxViewer";
    }
})();
