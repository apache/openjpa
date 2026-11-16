<!--
 Licensed to the Apache Software Foundation (ASF) under one
 or more contributor license agreements.  See the NOTICE file
 distributed with this work for additional information
 regarding copyright ownership.  The ASF licenses this file
 to you under the Apache License, Version 2.0 (the
 "License"); you may not use this file except in compliance
 with the License.  You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing,
 software distributed under the License is distributed on an
 "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 KIND, either express or implied.  See the License for the
 specific language governing permissions and limitations
 under the License.
-->
# Releasing Apache OpenJPA

This document describes how to release Apache OpenJPA: building and staging a release candidate,
the vote, and publishing the release. It covers two ways to stage and publish the release files:
through [Apache Trusted Releases (ATR)](#using-apache-trusted-releases-atr), or through the SVN
`dist` repositories as in earlier releases. The Maven build and the Nexus staging repository are the
same for both.

The general ASF rules apply. Read the
[release policy](https://www.apache.org/legal/release-policy.html),
[release signing](https://infra.apache.org/release-signing.html),
[release distribution](https://infra.apache.org/release-distribution.html) and
[Maven publishing](https://infra.apache.org/publishing-maven-artifacts.html) pages before your first release.

Version numbers follow `MAJOR.MINOR.PATCH`, see the
[OpenJPA release policy](https://openjpa.apache.org/openjpa-release-policy.html). The git tag is the plain
version, for example `4.1.1` (`tagNameFormat` `@{project.version}` in the root `pom.xml`).

## Contents

- [Preparation](#preparation)
- [Using Apache Trusted Releases (ATR)](#using-apache-trusted-releases-atr)
- [Setting up a vote](#setting-up-a-vote)
- [After the vote](#after-the-vote)
- [Local pre-checks before uploading to ATR](#local-pre-checks-before-uploading-to-atr)
- [Verifying a release candidate](#verifying-a-release-candidate)
- [Older release documentation](#older-release-documentation)

## Preparation

- An Apache account, and commit access to https://github.com/apache/openjpa.
- Log in once to https://repository.apache.org (Nexus) with your Apache ID.
- An OpenPGP key for signing, published to a key server (for example https://keys.openpgp.org) and listed in
  the OpenJPA KEYS file, https://dist.apache.org/repos/dist/release/openjpa/KEYS. If your key is not there yet:

  ```bash
  svn checkout --depth files https://dist.apache.org/repos/dist/release/openjpa/ openjpa-dist
  (gpg --list-sigs <YOUR_KEY_ID> && gpg --armor --export <YOUR_KEY_ID>) >> openjpa-dist/KEYS
  svn commit openjpa-dist/KEYS -m "Add the OpenPGP key of <YOUR NAME>"
  ```

  Only PMC members can commit to `dist/release`; ask one if you are not. New keys should be RSA with at
  least 4096 bits (ATR rejects DSA keys).
- The build signs with `maven-gpg-plugin` (`gpg.useagent` is `true` in the Apache parent POM), so
  `gpg` must be able to sign without a terminal prompt, for example through `gpg-agent` with a pinentry program.
  Make the release key your default key or set `gpg.keyname`.
- Maven needs your Apache credentials to deploy to Nexus. The server ids come from the Apache parent POM
  (`org.apache:apache`). Use [encrypted passwords](https://maven.apache.org/guides/mini/guide-encryption.html)
  in `~/.m2/settings.xml`:

  ```xml
  <settings>
    <servers>
      <!-- snapshots -->
      <server>
        <id>apache.snapshots.https</id>
        <username>YOUR_APACHE_ID</username>
        <password>YOUR_ENCRYPTED_PASSWORD</password>
      </server>
      <!-- release staging -->
      <server>
        <id>apache.releases.https</id>
        <username>YOUR_APACHE_ID</username>
        <password>YOUR_ENCRYPTED_PASSWORD</password>
      </server>
    </servers>
  </settings>
  ```
- Tools: git, a JDK 17 or newer (the build compiles for Java 17; CI builds with 17 and 21), Maven 3.9 or newer,
  gpg, svn (for KEYS and the SVN path), `python3`, `unzip`, `tar` and `curl` (for the [local pre-checks](#local-pre-checks-before-uploading-to-atr)),
  and the `atr` client if you use ATR from the command line.

Before you start:

- All JIRA issues for the version are resolved or moved to a later version:
  https://issues.apache.org/jira/projects/OPENJPA?selectedItem=com.atlassian.jira.jira-projects-plugin:release-page
- `openjpa-project/RELEASE-NOTES.html` has a section for the new version, with the JIRA release notes
  (the HTML from the release notes link of the version in JIRA) and a link to the release notes of the
  previous release in the table at the bottom.
- The copyright year in `NOTICE` and `openjpa-project/NOTICE` (`Copyright 2006-YYYY The Apache Software
  Foundation`) includes the current year.
- The text files that the archives ship are up to date. The assemblies include `README.txt` and `CHANGES.txt`
  of `openjpa-project` (`openjpa-project/src/assembly/components/notice.xml`): update both for the release
  (version, date, main changes), next to `openjpa-project/RELEASE-NOTES.html`.
- `mvn clean install` passes on the branch to be released, and the latest CI runs are green.

## Using Apache Trusted Releases (ATR)

[ATR](https://releases.apache.org/) is the ASF platform for staging, voting on and publishing releases.
It replaces the `dist/dev` staging area, runs checks on the uploaded files (signatures, checksums, LICENSE
and NOTICE, license headers, Apache RAT), sends the vote and announcement emails in your name, and publishes
the approved files to `dist/release`. It is in beta. ATR does **not** build or sign the artifacts and does
**not** publish to Maven Central: the Maven build, the signing and the Nexus staging repository work the
same with and without ATR.

> **Pick one path for each release, ATR or SVN, and never mix them.** The two paths differ at staging
> (step 6 of [Setting up a vote](#setting-up-a-vote)), at the vote email (step 9), and when publishing
> and announcing [after the vote](#after-the-vote). Decide before you start and follow only the matching
> variant in every step. Mixing them, for example staging in `dist/dev` and also starting the release in
> ATR, or running `svn mv` into `dist/release` while ATR holds the release, leaves two diverging copies of
> the release candidate. Inside ATR you can switch between the website and the command line.

Voting still happens on dev@openjpa.apache.org, and only votes of OpenJPA PMC members are binding.

### One-time setup

1. Register for the ASF multi-factor authentication at https://mfa.apache.org/.
2. You must be an OpenJPA PMC member, or be made a release manager by a PMC member on the OpenJPA
   committee page in ATR.
3. ATR must know your OpenPGP key. By default it imports the OpenJPA KEYS file, so adding your key to KEYS as
   described in [Preparation](#preparation) is enough. In this default KEYS mode (import from SVN) the KEYS file
   in `dist/release` is the only way: ATR refuses key uploads for the committee. Uploading a key at
   https://releases.apache.org/keys only works if the committee switched the KEYS mode on its committee page.
4. Get the credentials for the way you upload files:
   - website: your ASF login;
   - `rsync`: add an SSH **public** key at https://releases.apache.org/keys (not your OpenPGP key). You
     connect as your ASF id on port 2222;
   - command line: create a personal access token (PAT) at https://releases.apache.org/tokens (valid for 180 days).
5. To use the command line, install the client (it needs [uv](https://docs.astral.sh/uv/getting-started/installation/)) and sign in:

   ```bash
   uv tool install "apache-trusted-releases @ git+https://github.com/apache/tooling-releases-client"
   atr set asf.uid <your-asf-id>
   atr set tokens.pat
   atr jwt refresh
   ```

   `atr set tokens.pat` asks for the token without echoing it. `atr --help` and `atr docs` list all commands.
   The client is not stable yet; do not use it in unattended scripts.
6. The ATR project key of OpenJPA is `openjpa` (committee `openjpa`, see
   https://releases.apache.org/api/project/get/openjpa). Log in on the website and check that you see it.
7. The project settings in ATR are shared by all release managers and only need to be set once. Check them
   on the project page before the first ATR release:
   - **Vote** tab, **Start vote template URL**:
     `https://raw.githubusercontent.com/apache/openjpa/master/dev-tools/atr/start-vote-template.txt`.
     ATR fetches [the template](dev-tools/atr/start-vote-template.txt) whenever the vote form is shown, so a change
     to the file in `master` applies to the next vote. It uses the ATR `{{VARIABLES}}` listed under
     **Available template variables** on the settings page. The file is the body of the email, so it has no
     license header (it is excluded in `.rat-excludes` and in the RAT configuration of `pom.xml`).
   - **Vote** tab, **Start vote subject**: `[VOTE] Release Apache {{PROJECT_NAME}} {{VERSION}}`.
   - **Vote** tab, **Vote mode**: keep **Email**. OpenJPA votes by replying on dev@, and an email vote can only
     be resolved on the website.
   - **Metadata** tab, **Repositories**: `https://github.com/apache/openjpa`. ATR compares each source archive
     with the source commit only when the metadata lists a GitHub repository; the current value is the old
     `https://svn.apache.org/repos/asf/openjpa/`. The metadata can also be kept in the `project` block of
     `.asf.yaml` (ATR's project-configuration docs); OpenJPA's `.asf.yaml` does not have one.
   - **Finish** tab, **Announce release template URL**:
     `https://raw.githubusercontent.com/apache/openjpa/master/dev-tools/atr/announce-template.txt`
     ([the template](dev-tools/atr/announce-template.txt)), and **Announce release subject**:
     `[ANNOUNCE] Apache {{PROJECT_NAME}} {{VERSION}} released`.
   - **Finish** tab, **Default publish location** (the download path suffix): `{{VERSION}}`. Every OpenJPA
     release lives in a directory named after its version, for example `dist/release/openjpa/4.1.1/`.
     OpenJPA is the top-level project of its committee, so with the field empty ATR publishes the files
     straight into `dist/release/openjpa/`. Files cannot be moved once ATR has published them.
   - **Default vote recipient**: `dev@openjpa.apache.org`; **Default announce recipient**: `announce@apache.org`,
     with `dev@openjpa.apache.org` and `users@openjpa.apache.org` in CC.

   The [local pre-checks](#local-pre-checks-before-uploading-to-atr) use the `.rat-excludes` file that the source
   archives contain; ATR uses it as well, so no RAT excludes need to be set in the project settings.

### ATR operations at a glance

| Step | Website | Command line (`x.y.z` is the release version) |
|---|---|---|
| Start the release | **Draft → Candidates → Start a new release** | `atr release start openjpa x.y.z` |
| Upload the files | Compose page → **Upload files** (browser, `svn` or `rsync`) | `atr rsync openjpa x.y.z ./candidate/` or `atr upload openjpa x.y.z <path> <file>` |
| Review the checks | Compose page | `atr check wait\|status\|blockers\|concerns openjpa x.y.z` |
| Start the vote | Compose page → **Start voting** | `atr vote start openjpa x.y.z -m dev@openjpa.apache.org` |
| Read the tally | Vote page | `atr vote tabulate openjpa x.y.z` |
| Resolve the vote | Vote page → **Resolve vote** (the only way for email votes) | not available for email votes |
| Record the Maven Central release | Finish page → **Verify a third-party distribution** | `atr distribution record openjpa x.y.z MAVEN org.apache.openjpa openjpa x.y.z` |
| Publish to `dist/release` | Finish page → **Publish to ASF Distribution Area** | not available from the client; publish from the Finish page |
| Announce | Finish page → **Announce** | `atr announce openjpa x.y.z -m announce@apache.org` |

Without `--body` or `--body-file`, `atr vote start` and `atr announce` render the body from the templates
set in the project settings.

> **Do not use `atr vote start --auto-publish`.** With the client, ATR publishes to the top of the
> committee's distribution area, `dist/release/openjpa/`, and ignores the **Default publish location**, so the
> files would not end up in `dist/release/openjpa/x.y.z/` and cannot be moved afterwards.

The Apache parent POM (`org.apache:apache:40`) also has a `push-to-atr` profile. It runs the
`check-composing` and `upload` goals of `org.apache.tooling:atr-maven-plugin` (version `1.0.0-beta-1`) and
uploads `${project.artifactId}-${project.version}-source-release.zip` (`atr-release-type` is `zip`) with its
`.sha512` and `.asc` from `target/`, that is `openjpa-parent-x.y.z-source-release.zip`. It does not upload
`apache-openjpa-x.y.z-binary.zip`, which would still have to be uploaded by one of the other ways, and the
release must already be started in ATR. OpenJPA does not use the profile for now. TODO: try it before relying
on it: the plugin is a beta, needs a personal access token configured as a server in `~/.m2/settings.xml`
(see https://apache.github.io/tooling-atr-maven-plugin/), and the profile is inherited by every module of the
reactor, where no source release exists.

## Setting up a vote

> **Choose your path first: ATR or SVN, not both.** Steps marked **ATR** and **SVN** are alternatives.

For the first release of a new minor line (for example `4.2.0`), create its maintenance branch first, named
like the existing ones (`3.2.x`, `2.4.x`), and move `master` to the next minor version:

```bash
git checkout master && git pull --rebase
git checkout -b x.y.x
git push origin x.y.x
git checkout master
mvn versions:set -DnewVersion=x.<y+1>.0-SNAPSHOT -DgenerateBackupPoms=false
git commit -am "Set the version to x.<y+1>.0-SNAPSHOT" && git push origin master
```

TODO: check that `versions:set` updates every module (the root `pom.xml` does not configure
`versions-maven-plugin`). Make sure the next version exists in JIRA. Then release from the `x.y.x` branch.

1. Check out the branch to be released (`master` or a maintenance branch) with no local changes:

   ```bash
   git pull --rebase
   git status
   ```

2. Prepare the release. The configuration lives in the `release-prepare` and `release-perform` executions
   of `maven-release-plugin` in the root `pom.xml`, so the goals must be called with the execution id:

   ```bash
   mvn release:prepare@release-prepare
   ```

   This asks for the release version, the tag (the plain version, e.g. `4.2.0`) and the next development
   version, builds with `clean install -DskipTests`, and creates two `[maven-release-plugin]` commits and the
   tag **in your local repository only** (`pushChanges` is `false`). If it fails, run `mvn release:clean`,
   fix the problem and start again.

3. Build, sign and deploy the release from the tag:

   ```bash
   mvn release:perform@release-perform
   ```

   `localCheckout` is `true`, so this clones the tag from your local repository into `target/checkout` and
   runs `deploy -Ptest-derby -Dbuilddocs -DperformRelease` there, with the `apache-release` profile of the
   Apache parent POM. The tests run (on Derby). That build:
   - creates the source release `openjpa-parent-x.y.z-source-release.zip` with `.asc` and `.sha512`
     (`apache-release` profile of the Apache parent POM);
   - builds the manual from AsciiDoc and the assemblies of `openjpa-project` (`docbook-profile`, activated by
     `-Dbuilddocs`): `apache-openjpa-x.y.z-binary.zip`, `apache-openjpa-x.y.z-binary-nodocs.zip` and
     `apache-openjpa-x.y.z-source.zip`, attached with the classifiers `binary`, `binary-nodocs` and `source`
     and signed (`create-checksums` profile, activated by `performRelease`);
   - deploys all artifacts to a new staging repository in Nexus.

   Then push the release commits and the tag:

   ```bash
   git push origin <branch>
   git push origin x.y.z
   ```

4. Close the staging repository: log in to https://repository.apache.org, open **Staging Repositories**,
   select the `orgapacheopenjpa-NNNN` repository, check its content and **Close** it. Note its URL,
   `https://repository.apache.org/content/repositories/orgapacheopenjpa-NNNN/`, for the vote email.

5. Collect the files for `dist` in a new directory, for example `./candidate/`. OpenJPA has so far published
   the source release and the binary archive (see https://dist.apache.org/repos/dist/release/openjpa/4.1.1/).
   Take them from the closed staging repository, so the voted files are the Maven artifacts, and create the
   `.sha512` files where Nexus has none (do not copy the `.md5` or `.sha1` files):

   ```bash
   V=x.y.z
   REPO=https://repository.apache.org/content/repositories/orgapacheopenjpa-NNNN/org/apache/openjpa
   mkdir candidate && cd candidate
   for f in openjpa-parent/$V/openjpa-parent-$V-source-release.zip \
            apache-openjpa/$V/apache-openjpa-$V-binary.zip; do
       curl -fO "$REPO/$f"
       curl -fO "$REPO/$f.asc"
   done
   for f in *.zip; do shasum -a 512 "$f" > "$f.sha512"; done
   cd ..
   ```

   The same files are also in `target/checkout/target/` and `target/checkout/openjpa-project/target/site/downloads/`.

   The source archive in `dist` is `openjpa-parent-x.y.z-source-release.zip`, as for every release since 2.4.0
   except 3.2.1 and 3.2.2 (see https://archive.apache.org/dist/openjpa/). It is the ASF source release built by the
   `apache-release` profile, and it contains `.rat-excludes`, `LICENSE` and `NOTICE` at the top. Do not stage
   `apache-openjpa-x.y.z-source.zip` in addition; it stays on Maven Central only.

6. Run the [local pre-checks](#local-pre-checks-before-uploading-to-atr) on the candidate, then stage it.

   - **ATR:** start the release on the website (**Draft → Candidates → Start a new release**) or with
     `atr release start openjpa x.y.z`. A failed vote returns the release to the Compose phase, so the version
     stays the same and the `rcN` counter only appears in the vote email.
   - **SVN:** create `https://dist.apache.org/repos/dist/dev/openjpa/x.y.z/` (the layout of the earlier votes).

7. Upload or commit the files.

   - **ATR:** upload on the website (Compose page → **Upload files**, by browser, `svn` or `rsync`) or with
     `atr rsync openjpa x.y.z ./candidate/`. Then wait for the checks on the Compose page, or run:

     ```bash
     atr check wait openjpa x.y.z --timeout 300
     atr check status openjpa x.y.z --verbose
     atr check blockers openjpa x.y.z --members
     atr check concerns openjpa x.y.z --members
     ```

     Fix every blocker (re-sign, re-checksum and re-upload the files). Read every concern; you acknowledge the
     remaining ones when you start the vote.

     Once the project metadata lists the GitHub repository (see [One-time setup](#one-time-setup)), ATR compares
     the source archive with the commit it was built from, `git rev-parse x.y.z^{commit}`. The browser upload
     form asks for the commit. `atr rsync` and `atr upload` cannot send it; record it afterwards with the API
     call `release/commit` through the client's generic `atr api post`:

     ```bash
     atr api post /release/commit --project openjpa --version x.y.z --commit "$(git rev-parse x.y.z^{commit})"
     ```

     TODO: not tried yet. The client passes `--KEYWORD value` pairs as the JSON body, but its source notes a
     problem with passing `--version`; check the call with `atr api post --help`. Without the commit, ATR's
     compare check records a concern that must be acknowledged before the vote starts.
   - **SVN:**

     ```bash
     svn checkout https://dist.apache.org/repos/dist/dev/openjpa/x.y.z/ openjpa-rc
     cp candidate/* openjpa-rc/
     svn add openjpa-rc/*
     svn commit openjpa-rc -m "Add Apache OpenJPA x.y.z release candidate"
     ```

8. Look up the id of the version in JIRA: the number at the end of
   https://issues.apache.org/jira/projects/OPENJPA/versions/NNNNN, which you open from the
   [releases page](https://issues.apache.org/jira/projects/OPENJPA?selectedItem=com.atlassian.jira.jira-projects-plugin:release-page).

9. Start the vote on dev@openjpa.apache.org. It runs for at least 72 hours and passes with at least three
   binding +1 votes and more binding +1 than -1 votes
   ([ASF voting](https://www.apache.org/foundation/voting.html#ReleaseVotes)).

   - **ATR:** on the Compose page → **Start voting**. The form is filled from
     [`dev-tools/atr/start-vote-template.txt`](dev-tools/atr/start-vote-template.txt) with the ATR variables
     replaced. Replace `[REPO_NUM]` with the number of the staging repository and `[JIRA_VERSION_ID]` with the
     JIRA version id, review the text and send it. From the command line, write the body yourself (start from
     the template, with every value filled in):

     ```bash
     atr vote start openjpa x.y.z -m dev@openjpa.apache.org -d 72 \
         --subject "[VOTE] Release Apache OpenJPA x.y.z" --body-file vote.txt
     ```

     Add `--concerns-noted <keys>` to acknowledge the remaining concerns (`atr check concerns` lists the keys).
     Do not add `--auto-publish` (see [ATR operations at a glance](#atr-operations-at-a-glance)).
   - **SVN:** send the email yourself:

     ```text
     Subject: [VOTE] Release Apache OpenJPA x.y.z

     I've created a x.y.z release candidate, with the following artifacts up for a vote:

     Git tag (<commit>):
     https://github.com/apache/openjpa/tree/x.y.z

     List of changes:
     https://issues.apache.org/jira/projects/OPENJPA/versions/<JIRA_VERSION_ID>

     Staging artifacts:
     https://dist.apache.org/repos/dist/dev/openjpa/x.y.z/

     Maven staging repo:
     https://repository.apache.org/content/repositories/orgapacheopenjpa-NNNN/

     The release artifacts are signed with the key <KEY_ID> from:
     https://downloads.apache.org/openjpa/KEYS

     How to verify the release candidate:
     https://github.com/apache/openjpa/blob/master/RELEASING.md#verifying-a-release-candidate

     The vote is open for at least 72 hours.

     [ ] +1 approve
     [ ] +0 no opinion
     [ ] -1 disapprove (and reason why)
     ```

## After the vote

> Continue on the path you chose when you [set up the vote](#setting-up-a-vote).

### If the vote passes

1. Close the vote.
   - **ATR:** check ATR's tally (`atr vote tabulate openjpa x.y.z`) against the vote thread, then resolve the
     vote on the vote page (**Resolve vote**). ATR emails the result to the list, and a passed vote moves the
     release to the Finish phase.
   - **SVN:** send the result yourself, with the subject `[RESULT][VOTE] Release Apache OpenJPA x.y.z`, the
     counts of +1 (binding and non-binding), 0 and -1 votes, and a link to the vote thread on
     https://lists.apache.org/list.html?dev@openjpa.apache.org.

2. Release the staging repository in Nexus (https://repository.apache.org, **Staging Repositories**,
   **Release**). The artifacts reach Maven Central after some time.

3. Publish the files to https://dist.apache.org/repos/dist/release/openjpa/x.y.z/.
   - **ATR:** on the Finish page → **Publish to ASF Distribution Area**. Check that the publish location is
     `x.y.z` (from the **Default publish location** `{{VERSION}}`, see [One-time setup](#one-time-setup)) before you publish.
     Publish only from this page; the client has no publish command, and `atr vote start --auto-publish`
     would publish to `dist/release/openjpa/` instead of `dist/release/openjpa/x.y.z/`.
   - **SVN:**

     ```bash
     svn mv https://dist.apache.org/repos/dist/dev/openjpa/x.y.z \
            https://dist.apache.org/repos/dist/release/openjpa/x.y.z \
            -m "Release Apache OpenJPA x.y.z"
     ```

4. **ATR:** record the Maven Central release, on the Finish page (**Verify a third-party distribution**) or from
   the command line, once it is on Maven Central:

   ```bash
   atr distribution record openjpa x.y.z MAVEN org.apache.openjpa openjpa x.y.z
   ```

   ATR can require this before it lets you announce.

5. Release the version in JIRA: on the
   [releases page](https://issues.apache.org/jira/projects/OPENJPA?selectedItem=com.atlassian.jira.jira-projects-plugin:release-page)
   choose **Release** for x.y.z, and make sure the next version exists.

6. Add the release at https://reporter.apache.org/addrelease.html?openjpa if it is not listed there yet.

7. Update the website, https://github.com/apache/openjpa-site (see its README for building and publishing):
   - `content/downloads.md` on `main`: add the new release and remove the releases that are no longer in
     `dist/release`. Link the files through https://www.apache.org/dyn/closer.lua/openjpa/x.y.z/ and the
     `.asc` and `.sha512` files through https://downloads.apache.org/openjpa/x.y.z/.
   - `content/documentation.md` on `main`: add the manual, javadoc and release notes of the new version.
   - The `builds` branch holds the published manual, javadoc and release notes per version
     (`x.y.z/apache-openjpa/docs/`, `x.y.z/apidocs/`, `x.y.z/apache-openjpa/RELEASE-NOTES.html`) and has a
     helper script: `./add_docs.sh x.y.z apache-openjpa-x.y.z-binary.zip [javadoc directory or jar]`. It handles
     both the DocBook layout of the archives up to 4.1.x and the AsciiDoc layout from 4.2.0, which it copies to
     `x.y.z/apache-openjpa/docs/multihtml/manual.html` (multiple pages), `docs/html/manual.html` (single page) and
     `docs/manual.pdf`; link these in `content/documentation.md`. The script takes the javadoc from `docs/javadoc`
     of the binary archive. `release:perform` puts it there: with `-Dbuilddocs` the site of the root project is
     built, its javadoc report writes the aggregated javadoc to `target/site/apidocs`, and `openjpa-project` copies
     that directory to `docs/javadoc` of `apache-openjpa-x.y.z-binary.zip`. The aggregate leaves out the modules
     that skip javadoc (`openjpa-examples`, `openjpa-integration`, `openjpa-persistence-locking` and the packaging
     modules) and `openjpa-maven-plugin`.

     If the binary archive has no `docs/javadoc`, build the javadoc at the release tag and pass it as the third
     parameter:

     ```bash
     git checkout x.y.z
     mvn -DskipTests -Dbuilddocs package     # target/site/apidocs, and the binary archive with docs/javadoc
     ./add_docs.sh x.y.z apache-openjpa-x.y.z-binary.zip <openjpa checkout>/target/site/apidocs
     ```

     `mvn -DskipTests install` followed by `mvn javadoc:aggregate` builds the same javadoc into
     `target/reports/apidocs`. The `install` is needed there because `javadoc:aggregate` compiles the whole reactor
     again, and the OpenBooks example generates its metamodel with the `openjpa-all` jar of the same version from
     the local repository.

8. Wait until the files are on https://downloads.apache.org/openjpa/x.y.z/ and the artifacts on Maven Central,
   then announce the release from your `@apache.org` address to announce@apache.org,
   dev@openjpa.apache.org and users@openjpa.apache.org.
   - **ATR:** on the Finish page → **Announce**. The form is filled from
     [`dev-tools/atr/announce-template.txt`](dev-tools/atr/announce-template.txt). From the command line, with
     the body rendered from the same template:

     ```bash
     atr announce openjpa x.y.z -m announce@apache.org --cc dev@openjpa.apache.org --cc users@openjpa.apache.org
     ```
   - **SVN:** send the text of [`dev-tools/atr/announce-template.txt`](dev-tools/atr/announce-template.txt)
     yourself, with the `{{VARIABLES}}` filled in, and the subject `[ANNOUNCE] Apache OpenJPA x.y.z released`.

9. Remove the release that the new one replaces from `dist/release`; it stays available on
   https://archive.apache.org/dist/openjpa/. Releases of maintained branches stay. With ATR you can archive the
   older release from its page in ATR instead; otherwise:

   ```bash
   svn rm https://dist.apache.org/repos/dist/release/openjpa/<old-version> -m "Remove Apache OpenJPA <old-version>"
   ```

10. If the release was made from a maintenance branch, bring the changes that also belong on `master` over to `master`.

### If the vote fails

This covers a vote that ran its full time and failed, and a vote that the release manager withdraws early
(for example because a problem was found).

1. Record the result.
   - **ATR:** for a failed vote, resolve it as failed on the vote page (**Resolve vote**). To withdraw the
     vote early, cancel it on the website. ATR replies to the vote thread in both cases, so do not send a
     separate email. The release returns to the Compose phase, where you upload the next candidate; there are
     no staged files to delete.
   - **SVN:** for a failed vote, send `[RESULT][VOTE] Release Apache OpenJPA x.y.z` with the counts, as for a
     passed vote. To withdraw the vote early, reply to the vote thread with
     `[CANCEL][VOTE] Release Apache OpenJPA x.y.z` and the reason. Then remove the staged files:
     `svn rm https://dist.apache.org/repos/dist/dev/openjpa/x.y.z -m "Remove failed release candidate"`.
2. Drop the staging repository in Nexus.
3. Delete the tag (`git push --delete origin x.y.z`, `git tag -d x.y.z`) and revert the two
   `[maven-release-plugin]` commits, or reset the branch if they were not pushed yet.
4. Fix the problem and start again with [Setting up a vote](#setting-up-a-vote).

## Local pre-checks before uploading to ATR

[`dev-tools/atr/check-release.sh`](dev-tools/atr/check-release.sh) runs the main ATR checks on release archives
before they are uploaded, so that blockers and concerns show up before the release is started in ATR:

- `.asc` and `.sha512` (or `.sha256`) next to each archive, no `.md5`, the version in the file name;
- the checksum, and the signature, against your keyring or against the keys of a KEYS file (`-k`);
- LICENSE and NOTICE: a LICENSE that starts with the Apache License 2.0 text, and a NOTICE with
  `Apache <name>`, `Copyright <year>[-<year>] The Apache Software Foundation` and
  `This product includes software developed at The Apache Software Foundation` (in the root directory of a
  source archive; anywhere in a binary archive);
- license headers of source files, with the file patterns of ATR (`.txt`, `.xml`, `.properties` and other
  non-code files are not checked by ATR, so only RAT looks at them);
- Apache RAT on source archives, with the RAT version (0.18) and the command line of ATR and the `.rat-excludes`
  file of the archive. The RAT jar is taken from the local Maven repository or downloaded from Maven Central.

The checks follow `atr/tasks/checks/*.py` of https://github.com/apache/tooling-trusted-releases and are
implemented in [`dev-tools/atr/atr_checks.py`](dev-tools/atr/atr_checks.py). ATR runs more checks than these,
and its results are the ones that count.

```bash
dev-tools/atr/check-release.sh -v x.y.z -k https://downloads.apache.org/openjpa/KEYS candidate/*.zip
```

The script tells source from binary archives by the file name, as ATR does: a name with `source`, `src`,
`sources` or `project` is a source archive, a name with `bin`, `binary` or `binary-assembly` is a binary
archive, and a name with neither is treated as source. ATR also counts platform and distribution tokens and
directory names, which the script does not; OpenJPA's archive names are classified the same way by both.

The script needs bash, java, python3, unzip, tar, curl and gpg, prints `BLOCKER` and `CONCERN` lines like ATR, and
exits with 1 if it finds any. To try it before a release, build the archives from a working tree:

```bash
mvn clean install -DskipTests
mvn -pl openjpa-project package -Dbuilddocs -DskipTests        # openjpa-project/target/site/downloads/*.zip
mvn -N -Papache-release org.apache.maven.plugins:maven-assembly-plugin:single@source-release-assembly
                                                               # target/openjpa-parent-*-source-release.zip
```

Those archives have no `.asc` and `.sha512` files, so the script reports these as missing; create them with
`shasum -a 512` and `gpg --armor --detach-sign` if you want a clean run.

The vote and announce templates in `dev-tools/atr/` are the bodies of the emails and have no license header.
Only they are excluded from RAT, with `dev-tools/atr/*-template.txt` in `.rat-excludes` and in the
`apache-rat-plugin` configuration of `pom.xml` (keep both in sync); the scripts next to them have license
headers and are checked. ATR's header check does not look at `.txt` files.

## Verifying a release candidate

Everyone is welcome to check a release candidate and vote; votes of PMC members are binding. When you vote,
say what you checked. A typical review:

1. Get the files from the link in the vote email: the ATR vote page (which also shows ATR's check results;
   the files can be downloaded in the browser or with `curl` without a login) or
   https://dist.apache.org/repos/dist/dev/openjpa/x.y.z/.
2. Check the signatures and checksums, against the OpenJPA KEYS file:

   ```bash
   curl -fO https://downloads.apache.org/openjpa/KEYS
   gpg --import KEYS
   for f in *.zip; do gpg --verify "$f.asc" "$f" && shasum -a 512 -c "$f.sha512"; done
   ```

   A `.sha512` file that contains only the hash cannot be read by `shasum -c`; compare the output of
   `shasum -a 512 <file>` with it instead. With a checkout of the OpenJPA repository,
   `dev-tools/atr/check-release.sh -k KEYS *.zip` runs these checks together with the LICENSE, NOTICE,
   header and RAT checks.
3. Check the source archive: LICENSE and NOTICE are correct, it contains no compiled code, and it matches the
   git tag (for example `diff -r` against `git archive x.y.z` extracted, apart from generated files).
4. Run Apache RAT on the extracted source archive, either with the script above or with the build:
   `mvn apache-rat:check` from the extracted directory.
5. Build from the source archive and run the tests: `mvn clean install` (Derby, the default), or with other
   databases through the `test-*` profiles of the root `pom.xml`.
6. Try the binary archive or the staged Maven artifacts with your application, the examples in the binary
   archive, or the Jakarta Persistence TCK.

## Older release documentation

This file replaces the release documentation that is spread over the website and
`openjpa-project/RELEASE_STEPS.adoc`. The following pages of https://openjpa.apache.org
(sources in `content/` of https://github.com/apache/openjpa-site) describe older processes (SVN tags, the
Apache CMS, MD5 checksums, Java 6) and are superseded by this file:

| Page | Status |
|---|---|
| [release-management.md](https://openjpa.apache.org/release-management.html) | superseded; index of the pages below |
| [release-setup.md](https://openjpa.apache.org/release-setup.html) | superseded by [Preparation](#preparation) |
| [apache-nexus-release-process-_1.2.x-2.1.x_.md](https://openjpa.apache.org/apache-nexus-release-process-_1.2.x-2.1.x_.html) | superseded by [Setting up a vote](#setting-up-a-vote) and [After the vote](#after-the-vote) |
| [update-release-text-files.md](https://openjpa.apache.org/update-release-text-files.html) | superseded by [Preparation](#preparation) (release notes) |
| [release-artifact-locations.md](https://openjpa.apache.org/release-artifact-locations.html) | superseded; the locations are listed in this file |
| [verifying-release-signatures.md](https://openjpa.apache.org/verifying-release-signatures.html) | superseded by [Verifying a release candidate](#verifying-a-release-candidate) |
| [recovering-from-a-vetoed-release.md](https://openjpa.apache.org/recovering-from-a-vetoed-release.html) | superseded by [If the vote fails](#if-the-vote-fails) |
| [openjpa-release-policy.md](https://openjpa.apache.org/openjpa-release-policy.html) | still valid (version numbering and compatibility) |

After the 4.2.0 release, replace these pages on the website with a link to this file and remove
`openjpa-project/RELEASE_STEPS.adoc`.
