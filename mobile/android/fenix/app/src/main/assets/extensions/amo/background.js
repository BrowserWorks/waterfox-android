/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

// Override User-Agent for addons.mozilla.org to prevent mobile redirects
// BUT only for non-android URLs
browser.webRequest.onBeforeSendHeaders.addListener(
  function(details) {
    // Don't override UA for android-specific URLs
    if (details.url.includes('/android/')) {
      return;
    }

    const headers = details.requestHeaders;

    // Find and replace the User-Agent header
    for (let i = 0; i < headers.length; i++) {
      if (headers[i].name.toLowerCase() === 'user-agent') {
        headers[i].value = 'Mozilla/5.0 (X11; Linux x86_64; rv:140.0) Gecko/20100101 Firefox/140.0';
        break;
      }
    }

    return { requestHeaders: headers };
  },
  { urls: ["*://addons.mozilla.org/*"] },
  ["blocking", "requestHeaders"]
);
