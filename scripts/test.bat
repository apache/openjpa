@setlocal
pushd openjpa-persistence-jdbc
mvn test -Dtest=%1 %2 %3 %4
popd
@endlocal
