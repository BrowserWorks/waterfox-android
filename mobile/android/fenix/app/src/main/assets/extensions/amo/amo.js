/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

// Function to add Android compatibility banner
function addAndroidCompatibilityBanner() {
  // Check if banner already exists
  if (document.querySelector('.WrongPlatformWarning.HeroRecommendation-WrongPlatformWarning')) {
    return;
  }

  // Find the HeroRecommendation wrapper
  const heroWrapper = document.querySelector('.HeroRecommendation-wrapper');
  if (!heroWrapper) {
    return;
  }

  // Find the AppBanner to insert after it
  const appBanner = heroWrapper.querySelector('.AppBanner.HeroRecommendation-banner');

  // Create the banner HTML
  const bannerHTML = `
    <div class="WrongPlatformWarning HeroRecommendation-WrongPlatformWarning">
      <div class="Notice Notice-warningInfo">
        <div class="Notice-icon"></div>
        <div class="Notice-column">
          <div class="Notice-content">
            <p class="Notice-text">
              <span class="WrongPlatformWarning-message">
                Not all extensions are compatible with Waterfox for Android. To view only compatible extensions, <a href="/android/">click here</a>.
              </span>
            </p>
          </div>
        </div>
      </div>
    </div>
  `;

  // Insert the banner after the AppBanner, or at the beginning if AppBanner doesn't exist
  if (appBanner) {
    appBanner.insertAdjacentHTML('afterend', bannerHTML);
  } else {
    heroWrapper.insertAdjacentHTML('afterbegin', bannerHTML);
  }
}

// Function to replace 'Firefox' with 'Waterfox' on install buttons
function replaceInstallButtonText() {
  const installButtons = document.querySelectorAll('.AMInstallButton-button');
  for (const button of installButtons) {
    if (button.textContent.includes('Firefox')) {
      button.textContent = button.textContent.replace(/Firefox/g, 'Waterfox');
    }
  }
}

// Function to replace 'Firefox' with 'Waterfox' in Android compatibility badges
function replaceAndroidBadgeText() {
  const badges = document.querySelectorAll('.Badge.Badge-android-compatible');
  badges.forEach(badge => {
    const walker = document.createTreeWalker(badge, NodeFilter.SHOW_TEXT);
    let node;
    while ((node = walker.nextNode())) {
      if (node.nodeValue.includes('Firefox')) {
        node.nodeValue = node.nodeValue.replace(/Firefox/g, 'Waterfox');
      }
    }
  });
}

function runModifications() {
    addAndroidCompatibilityBanner();
    replaceInstallButtonText();
    replaceAndroidBadgeText();
}

// Run functions immediately
runModifications();

// Monitor for DOM changes
const observer = new MutationObserver(runModifications);
observer.observe(document.body, { childList: true, subtree: true });