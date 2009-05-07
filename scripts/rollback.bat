set REPOS=%1
set FILE=%2
set VERSION=%3
set URL=%1/%2
svn merge %URL%@HEAD %URL%@%VERSION%  %FILE%

@REM merge svn://domain.com/repo/trunk/folder/changedFile.txt@HEAD svn://domain.com/repo/trunk/folder/changedFile.txt@215 ./folder/changedFile.txt