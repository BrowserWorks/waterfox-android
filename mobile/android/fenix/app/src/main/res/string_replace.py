#!/usr/bin/env python3

"""
Comprehensive brand replacement script for Waterfox Android strings.
This script handles all the transformations needed when rebasing:
- Firefox → Waterfox
- Mozilla → BrowserWorks (except Mozilla Account references)
- Fixes format specifiers (%s: %d → %1$s: %2$d)
- Fixes apostrophe escaping for Android strings
- Handles various unicode punctuation marks
"""

import os
import re
import glob

def process_string_files():
    """Complete brand replacement and format fixing for all string files"""

    # Get all string files
    pattern = "mobile/android/fenix/app/src/main/res/values*/strings.xml"
    files = glob.glob(pattern)

    if not files:
        print("No strings.xml files found!")
        return

    print(f"Found {len(files)} strings.xml files to process")

    success_count = 0
    for file_path in files:
        print(f"Processing: {file_path}")
        if process_file(file_path):
            success_count += 1

    print(f"\nSuccessfully processed {success_count}/{len(files)} files")

def process_file(file_path):
    """Process a single string file"""
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            content = f.read()

        # 1. Replace brand names in string values only
        content = replace_brand_names(content)

        # 2. Fix format specifiers for trackers blocked panel
        content = fix_format_specifiers(content)

        # 3. Fix apostrophe escaping for Android strings
        content = fix_apostrophes(content)

        # 4. Clean up any artifacts from failed sed commands
        content = content.replace('">>','">')

        with open(file_path, 'w', encoding='utf-8') as f:
            f.write(content)

        return True

    except Exception as e:
        print(f"Error processing {file_path}: {e}")
        return False

def replace_brand_names(content):
    """Replace brand names in string values only, preserving Mozilla Account references"""

    def replace_in_string(match):
        start_tag = match.group(1)
        string_content = match.group(2)
        end_tag = match.group(3)

        # Always replace Firefox with Waterfox
        string_content = string_content.replace('Firefox', 'Waterfox')

        # Replace Mozilla with BrowserWorks, but preserve "Mozilla Account" references
        if 'Mozilla Account' not in string_content:
            string_content = string_content.replace('Mozilla', 'BrowserWorks')

        return start_tag + string_content + end_tag

    # Apply replacements only to string values
    content = re.sub(
        r'(<string[^>]*>)(.*?)(</string>)',
        replace_in_string,
        content,
        flags=re.DOTALL
    )

    return content

def fix_format_specifiers(content):
    """Fix format specifiers for trackers_blocked_panel_categorical_num_trackers_blocked"""

    # This handles various punctuation marks between %s and %d:
    # - Regular colon (:)
    # - Amharic colon (፡)
    # - Full-width colon (：)
    # - With or without spaces
    # - Additional text after %d (like Korean "개" or Chinese "組")

    def replace_format_specifiers(match):
        start_tag = match.group(1)
        middle_text = match.group(2)  # The text between %s and %d
        end_tag = match.group(3)
        return f"{start_tag}%1$s{middle_text}%2$d{end_tag}"

    pattern = r'(<string name="trackers_blocked_panel_categorical_num_trackers_blocked"[^>]*>)%s([^%]*?)%d(</string>)'
    content = re.sub(pattern, replace_format_specifiers, content)

    return content

def fix_apostrophes(content):
    """Fix apostrophe escaping in Android strings"""

    # Define patterns that need apostrophe escaping
    apostrophe_patterns = [
        # Possessive forms
        (r"BrowserWorks's", r"BrowserWorks\'s"),

        # Common contractions
        (r"you're", r"you\'re"),
        (r"you'll", r"you\'ll"),
        (r"we're", r"we\'re"),
        (r"we'll", r"we\'ll"),
        (r"they're", r"they\'re"),
        (r"they'll", r"they\'ll"),
        (r"it's", r"it\'s"),
        (r"that's", r"that\'s"),
        (r"what's", r"what\'s"),
        (r"here's", r"here\'s"),
        (r"there's", r"there\'s"),
        (r"let's", r"let\'s"),
        (r"don't", r"don\'t"),
        (r"doesn't", r"doesn\'t"),
        (r"won't", r"won\'t"),
        (r"can't", r"can\'t"),
        (r"couldn't", r"couldn\'t"),
        (r"wouldn't", r"wouldn\'t"),
        (r"shouldn't", r"shouldn\'t"),
        (r"mustn't", r"mustn\'t"),
        (r"haven't", r"haven\'t"),
        (r"hasn't", r"hasn\'t"),
        (r"hadn't", r"hadn\'t"),
        (r"isn't", r"isn\'t"),
        (r"wasn't", r"wasn\'t"),
        (r"weren't", r"weren\'t"),
        (r"aren't", r"aren\'t"),
    ]

    # Apply apostrophe escaping only within string values
    def fix_apostrophes_in_string(match):
        start_tag = match.group(1)
        string_content = match.group(2)
        end_tag = match.group(3)

        # Apply all apostrophe fixes
        for pattern, replacement in apostrophe_patterns:
            string_content = re.sub(pattern, replacement, string_content, flags=re.IGNORECASE)

        return start_tag + string_content + end_tag

    content = re.sub(
        r'(<string[^>]*>)(.*?)(</string>)',
        fix_apostrophes_in_string,
        content,
        flags=re.DOTALL
    )

    return content

def main():
    """Main function"""
    print("Starting comprehensive brand replacement...")
    print("This script will:")
    print("1. Replace Firefox → Waterfox")
    print("2. Replace Mozilla → BrowserWorks (preserving Mozilla Account references)")
    print("3. Fix format specifiers to use positional format")
    print("4. Fix apostrophe escaping for Android strings")
    print("5. Clean up any artifacts")
    print()

    process_string_files()

    print("\nBrand replacement completed!")
    print("\nVerification commands:")
    print("grep -r '>.*Waterfox.*</string>' mobile/android/fenix/app/src/main/res/values*/strings.xml | wc -l")
    print("grep -r '>.*BrowserWorks.*</string>' mobile/android/fenix/app/src/main/res/values*/strings.xml | wc -l")
    print("grep -r 'Mozilla account' mobile/android/fenix/app/src/main/res/values*/strings.xml | wc -l")

if __name__ == "__main__":
    main()
