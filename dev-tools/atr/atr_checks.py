#!/usr/bin/env python3
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.

"""Local copies of some of the checks that Apache Trusted Releases (ATR) runs on a release candidate.

Used by check-release.sh. The logic follows atr/tasks/checks/license.py, rat.py and hashing.py
of https://github.com/apache/tooling-trusted-releases (and paths.py for the version in the file name); keep it in sync when ATR changes.

  atr_checks.py headers DIR                 license headers of source files (license.py, headers)
  atr_checks.py license-files DIR source|binary
                                            LICENSE and NOTICE files (license.py, files)
  atr_checks.py rat-report XML              evaluate an Apache RAT XML report (rat.py)
  atr_checks.py hash FILE.sha512            compare a checksum file with its artifact (hashing.py)
  atr_checks.py filename-version NAME VERSION
                                            the release version in an artifact file name (paths.py)

DIR is the directory an archive was extracted into (it holds the single root directory of the archive).
Each command prints one line per finding, prefixed with BLOCKER, CONCERN or OK, and exits with 1 if
there is a BLOCKER or CONCERN.
"""

import hashlib
import os
import pathlib
import re
import sys
import xml.etree.ElementTree as ElementTree

# atr/tasks/checks/license.py: HTTP_APACHE_LICENSE_HEADER
HTTP_APACHE_LICENSE_HEADER = (
    b"Licensed to the Apache Software Foundation ASF under one or mor"
    b"e contributor license agreements See the NOTICE file distribute"
    b"d with this work for additional information regarding copyright"
    b" ownership The ASF licenses this file to you under the Apache L"
    b"icense Version 2 0 the License you may not use this file except"
    b" in compliance with the License You may obtain a copy of the Li"
    b"cense at http www apache org licenses LICENSE 2 0 Unless requir"
    b"ed by applicable law or agreed to in writing software distribut"
    b"ed under the License"
)
HTTPS_APACHE_LICENSE_HEADER = HTTP_APACHE_LICENSE_HEADER.replace(b" http ", b" https ")

SPDX_APACHE_LICENSE_PATTERNS = (
    rb"^[^\w\r\n]*SPDX-License-Identifier: Apache-2\.0 *(?:\*/|-->|#>)? *\r?$",
    rb"^[^\w\r\n]*SPDX-FileCopyrightText: See the NOTICE file distributed with this work"
    rb" for additional information regarding copyright ownership[^\w\r\n]*\r?$",
    rb"^[^\w\r\n]*SPDX-FileContributor: Licensed to the Apache Software Foundation \(ASF\)"
    rb" under one or more contributor license agreements[^\w\r\n]*\r?$",
)

# atr/tasks/checks/license.py: INCLUDED_PATTERNS, the only files whose headers ATR checks
INCLUDED_PATTERNS = [
    r"\.(js|mjs|cjs|jsx)$",
    r"\.py$",
    r"\.(sql|ddl|dml)$",
    r"\.(ts|tsx|mts|cts)$",
    r"\.(sh|bash|zsh|ksh)$",
    r"\.(java|jav)$",
    r"\.(cs|csx)$",
    r"\.(cpp|cxx|cc|c\+\+|hpp)$",
    r"\.(c|h)$",
    r"\.(php|php[3-9]|phtml)$",
    r"\.(ps1|psm1|psd1)$",
    r"\.go$",
    r"\.rs$",
    r"\.(kt|kts)$",
    r"\.(lua)$",
    r"\.dart$",
    r"\.(asm|s|S)$",
    r"\.(rb|rbw)$",
    r"\.swift$",
    r"\.(r|R)$",
    r"\.(vb|vbs)$",
    r"\.m$",
    r"\.vba$",
    r"\.(groovy|gvy|gy|gsh)$",
    r"\.(scala|sc)$",
    r"\.(pl|pm|t)$",
    r"\.(bat|cmd)$",
    r"\.dtd$",
]

# atr/constants.py: GENERATED_FILE_SUFFIXES
GENERATED_FILE_SUFFIXES = (".bundle.js", ".chunk.js", ".css.map", ".js.map", ".min.css", ".min.js", ".min.map")

# ATR compares the start of LICENSE with its copy of the Apache License 2.0 (atr/constants.py,
# APACHE_LICENSE_2_0) after stripping each line and dropping blank lines. These are the number of
# such lines and the SHA-256 of them joined with "\n", computed from that constant.
APACHE_LICENSE_LINES = 169
APACHE_LICENSE_SHA256 = "265250f9d8fe298dc073418e393183e326e65f9ce8b3a5382334bb09ac2522b5"
APACHE_LICENSE_URLS = {
    "https://www.apache.org/licenses/": "http://www.apache.org/licenses/",
    "https://www.apache.org/licenses/LICENSE-2.0": "http://www.apache.org/licenses/LICENSE-2.0",
}
MAX_LICENSE_NOTICE_SIZE = 1024 * 1024


class Report:
    def __init__(self):
        self.failed = False

    def ok(self, message):
        print(f"OK       {message}")

    def concern(self, message):
        self.failed = True
        print(f"CONCERN  {message}")

    def blocker(self, message):
        self.failed = True
        print(f"BLOCKER  {message}")


def headers_validate(content):
    """atr/tasks/checks/license.py: headers_validate"""
    # The markers are split so that Apache RAT does not take this file for a generated one
    for pattern in (b"Generated " b"By:JJTree", b"Generated " b"By:JavaCC"):
        if pattern in content:
            return True
    content = re.sub(rb"^[ \t]*@?rem\b", b"", content, flags=re.MULTILINE | re.IGNORECASE)
    spdx = re.sub(rb"[ \t]+", b" ", content)
    if all(re.search(p, spdx, re.MULTILINE | re.IGNORECASE) for p in SPDX_APACHE_LICENSE_PATTERNS):
        return True
    content = re.sub(rb"[ \t\r\n]+", b" ", content)
    for span in re.finditer(rb"Licensed to the.*?under the License", content, re.MULTILINE):
        joined = b" ".join(re.findall(rb"[A-Za-z0-9]+", span.group(0))).lower()
        if joined in (HTTP_APACHE_LICENSE_HEADER.lower(), HTTPS_APACHE_LICENSE_HEADER.lower()):
            return True
    return False


def should_check_header(rel_path, file_path):
    if rel_path.endswith(GENERATED_FILE_SUFFIXES):
        return False
    if os.path.splitext(rel_path)[1] and any(re.search(p, rel_path, re.IGNORECASE) for p in INCLUDED_PATTERNS):
        return True
    # Files starting with a shebang are checked whatever their name
    with open(file_path, "rb") as f:
        return f.read(2) == b"#!"


def cmd_headers(archive_dir):
    report = Report()
    checked = valid = skipped = 0
    for dirpath, dirnames, filenames in os.walk(archive_dir):
        dirnames.sort()
        for filename in sorted(filenames):
            if filename.startswith("._"):
                continue
            file_path = os.path.join(dirpath, filename)
            rel_path = os.path.relpath(file_path, archive_dir)
            if (not os.path.isfile(file_path)) or (not should_check_header(rel_path, file_path)):
                skipped += 1
                continue
            with open(file_path, "rb") as f:
                content = f.read(4096)
            if (len(content) < 4096) and (not content.strip()):
                skipped += 1
                continue
            checked += 1
            if headers_validate(content):
                valid += 1
            else:
                report.concern(f"{rel_path}: Could not find Apache License header")
    print(f"Checked {checked} files, found {valid} with valid headers, "
          f"{checked - valid} with invalid headers, and {skipped} skipped")
    return report


def license_problem(path):
    with open(path, "rb") as f:
        text = f.read(MAX_LICENSE_NOTICE_SIZE).decode("utf-8", errors="replace")
    lines = [APACHE_LICENSE_URLS.get(line.strip(), line.strip()) for line in text.splitlines() if line.strip()]
    lines = lines[:APACHE_LICENSE_LINES]
    if hashlib.sha256("\n".join(lines).encode("utf-8")).hexdigest() != APACHE_LICENSE_SHA256:
        return "does not start with the text of https://www.apache.org/licenses/LICENSE-2.0.txt"
    return None


def notice_problems(path):
    """atr/tasks/checks/license.py: _files_check_core_logic_notice"""
    with open(path, "rb") as f:
        raw = f.read(MAX_LICENSE_NOTICE_SIZE)
    try:
        content = raw.decode("utf-8")
    except UnicodeDecodeError:
        return ["the NOTICE file is not valid UTF-8"]
    normalised = re.sub(r"\s+", " ", content)
    issues = []
    if not re.search(r"Apache [\w\-\.]+", normalised):
        issues.append("missing or invalid Apache product header")
    if not re.search(r"Copyright (\d{4}|\d{4}-\d{4}) The Apache Software Foundation", normalised):
        issues.append("missing or invalid copyright statement")
    if not re.search(r"This product includes software developed at The Apache Software Foundation", normalised):
        issues.append("missing or invalid foundation attribution")
    return issues


def cmd_license_files(archive_dir, kind):
    report = Report()
    roots = [e for e in sorted(os.listdir(archive_dir))
             if (not e.startswith("._")) and os.path.isdir(os.path.join(archive_dir, e))]
    if len(roots) != 1:
        report.concern(f"Expected single root directory, found {len(roots)}")
        return report
    root = pathlib.Path(archive_dir) / roots[0]

    if kind == "source":
        # Only LICENSE and NOTICE directly in the root directory count
        license_path, notice_path = root / "LICENSE", root / "NOTICE"
        if not license_path.is_file():
            report.blocker("No LICENSE file found")
        elif (problem := license_problem(license_path)) is not None:
            report.blocker(f"LICENSE is invalid: {problem}")
        else:
            report.ok("LICENSE is valid")
        if not notice_path.is_file():
            report.blocker("No NOTICE file found")
        elif issues := notice_problems(notice_path):
            report.concern(f"NOTICE is invalid: {', '.join(issues)}")
        else:
            report.ok("NOTICE is valid")
        return report

    # Binary archives: any LICENSE(.txt) and NOTICE(.txt) in the tree, one valid file of each is enough
    licenses, notices = [], []
    for dirpath, dirnames, filenames in os.walk(root):
        dirnames[:] = sorted(d for d in dirnames if not d.startswith("._"))
        for filename in sorted(filenames):
            if filename in ("LICENSE", "LICENSE.txt"):
                licenses.append(pathlib.Path(dirpath) / filename)
            if filename in ("NOTICE", "NOTICE.txt"):
                notices.append(pathlib.Path(dirpath) / filename)
    valid_license = next((p for p in licenses if license_problem(p) is None), None)
    if not licenses:
        report.blocker("No LICENSE or LICENSE.txt file found")
    elif valid_license is None:
        report.blocker("No valid LICENSE or LICENSE.txt file found")
    else:
        report.ok(f"{valid_license.relative_to(root)} is valid")
    valid_notice = next((p for p in notices if not notice_problems(p)), None)
    if not notices:
        report.blocker("No NOTICE or NOTICE.txt file found")
    elif valid_notice is None:
        report.concern("No valid NOTICE or NOTICE.txt file found")
    else:
        report.ok(f"{valid_notice.relative_to(root)} is valid")
    return report


def cmd_rat_report(xml_file):
    """atr/tasks/checks/rat.py: _synchronous_core_parse_output_core"""
    report = Report()
    root = ElementTree.parse(xml_file).getroot()
    if (root is None) or (root.tag != "rat-report") or (root.find("version") is None):
        report.blocker(f"{xml_file} is not a RAT report")
        return report
    total = approved = 0
    for resource in root.iter("resource"):
        total += 1
        name = resource.get("name", "").lstrip("/")
        if name.startswith("./"):
            name = name[2:]
        license_elem = resource.find("license")
        if license_elem is None:
            if resource.get("type", "") in {"NOTICE", "BINARY", "IGNORED", "ARCHIVE"}:
                approved += 1
            else:
                report.concern(f"{name}: Unknown license")
        elif resource.find("license[@approval='true']") is not None:
            approved += 1
        else:
            license_name = license_elem.get("name", "Unknown")
            if license_name == "Unknown license":
                report.concern(f"{name}: Unknown license")
            else:
                report.concern(f"{name}: Unapproved license ({license_name})")
    print(f"RAT {root.find('version').get('version', '')}: {total} files, {approved} approved")
    return report


def cmd_hash(hash_file):
    """atr/tasks/checks/hashing.py"""
    report = Report()
    algorithm = os.path.splitext(hash_file)[1].lstrip(".")
    if algorithm not in ("sha256", "sha512"):
        report.blocker(f"{hash_file}: unsupported hash algorithm {algorithm}")
        return report
    artifact = hash_file[: -(len(algorithm) + 1)]
    if not os.path.isfile(artifact):
        report.blocker(f"{hash_file}: referenced artifact not found")
        return report
    h = hashlib.sha256() if algorithm == "sha256" else hashlib.sha512()
    with open(artifact, "rb") as f:
        while chunk := f.read(1 << 20):
            h.update(chunk)
    with open(hash_file, encoding="utf-8") as f:
        expected = f.read()
    parts = expected.strip().split()
    name = os.path.basename(artifact)
    if not parts:
        report.blocker(f"{hash_file}: empty checksum file")
        return report
    if (len(parts) > 3) and (parts[0] == "SHA512") and (parts[2] == "="):
        value = parts[3].lower()
    elif expected.startswith(name):
        value = expected.removeprefix(name + ":").replace(" ", "").replace("\n", "").lower()
    else:
        value = parts[0].lower()
    if value == h.hexdigest():
        report.ok(f"{os.path.basename(hash_file)}: hash ({algorithm}) matches")
    else:
        report.blocker(f"{os.path.basename(hash_file)}: hash ({algorithm}) mismatch")
    return report


def filename_has_version(filename, version):
    """atr/tasks/checks/paths.py: _filename_has_version

    A digit, or a digit and a dot, on either side makes it a different version, so 1.2 does not
    match in 11.2, 0.1.2 or 1.2.3. Case is ignored.
    """
    pattern = r"(?<![0-9])(?<![0-9]\.)" + re.escape(version) + r"(?![0-9])(?!\.[0-9])"
    return re.search(pattern, filename, re.IGNORECASE) is not None


def cmd_filename_version(name, version):
    report = Report()
    if filename_has_version(name, version):
        report.ok(f"{name}: file name includes the release version '{version}'")
    else:
        report.blocker(f"Artifact filename must include the release version '{version}'")
    return report


def main(argv):
    if len(argv) == 3 and argv[1] == "headers":
        report = cmd_headers(argv[2])
    elif len(argv) == 4 and argv[1] == "license-files" and argv[3] in ("source", "binary"):
        report = cmd_license_files(argv[2], argv[3])
    elif len(argv) == 3 and argv[1] == "rat-report":
        report = cmd_rat_report(argv[2])
    elif len(argv) == 3 and argv[1] == "hash":
        report = cmd_hash(argv[2])
    elif len(argv) == 4 and argv[1] == "filename-version":
        report = cmd_filename_version(argv[2], argv[3])
    else:
        print(__doc__, file=sys.stderr)
        return 2
    return 1 if report.failed else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
