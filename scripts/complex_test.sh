#!/bin/bash

SCRIPT_ROOT=$(cd -- $(dirname "${0}") && pwd)
PROJECT_ROOT="$(cd "${SCRIPT_ROOT}/.." && pwd)"
declare -A FULL_DB_LIST=(
	[h2-2]=embedded
	[derby]=embedded
	[hsqldb]=embedded
	[mysql]=docker
	[mariadb]=docker
	[postgresql]=docker
	[mssql]=docker
	[herddb]=docker
	[oracle]=docker
)
declare -A VERSIONS=(
	[mysql]='8.0 8.4.8 9.4.0'
	[postgresql]='11 16 18'
)
MATRIX=${!FULL_DB_LIST[@]}
REPORT_DIR=.report
START_WITH=''

usage() {
	echo "usage ${0} [-s|--start-with DB_ALIAS] [-r|--report-dir DIR] [-m|--matrix user_matrix] [-h|--help]"
	echo -e "\t-s|--start-with DB_ALIAS -- option to skip some databases in the begininning (continue from) [def: '${START_WITH}']"
	echo -e "\t-r|--report-dir DIR -- the folder with report and logs (will be autocreated) [def: '${REPORT_DIR}']"
	echo -e "\t-m|--matrix user_matrix -- test matrix in the format 'db1:ver1,ver2;db2;db3:ver3;...' [def db list: ${MATRIX[@]}]"
	echo -e "\ti.e DB delimiter is ';', in case version(s) should be specified it MUST follow after ':' and have ',' as delimiter"
	echo -e "\texample: '-m=oracle' or '--matrix=oracle;mysql:8.0' or '--matrix postgresql:16,18;mysql:9.4.0'"
	echo -e "\t-h|--help -- this help message"
}

user_matrix=''

while [[ $# -gt 0 ]]; do
	case ${1} in
		-s=*|--start-with=*)
			START_WITH="${1#*=}"
			shift
			;;
		-s|--start-with)
			START_WITH="${2}"
			shift 2
			;;
		-r=*|--report-dir=*)
			REPORT_DIR="${1#*=}"
			shift
			;;
		-r|--report-dir)
			REPORT_DIR="${2}"
			shift 2
			;;
		-m=*|--matrix=*)
			user_matrix="${1#*=}"
			shift
			;;
		-m|--matrix)
			user_matrix="${2}"
			shift 2
			;;
		-h|--help)
			usage
			exit 0
			shift
			;;
		*)
			echo "Unknown option ${1}"
			usage
			exit 1
			;;
	esac
done

if [[ -n "${user_matrix}" ]]; then
	MATRIX=()
	IFS=';' read -r -a dbvers <<< "${user_matrix}"
	for dbver in ${dbvers[@]}; do
		if [[ -z "${dbver}" ]]; then
			continue
		fi
		IFS=':' read -r -a db_vers <<< "${dbver}"
		if [[ ${#db_vers[@]} == 1 ]]; then
			db=${db_vers[0]}
			vers=''
		else
			db=${db_vers[0]}
			vers=${db_vers[1]}
		fi
		MATRIX+=(${db})
		if [[ -n "${vers}" ]]; then
			VERSIONS+=([${db}]=$(echo -n "${vers}" | sed -r 's/[,]/ /g'))
		fi
	done
fi

REPORT_DIR="${SCRIPT_ROOT}/${REPORT_DIR}"
REPORT_FILE="${REPORT_DIR}/report.txt"
if [[ -d "${REPORT_DIR}" ]]; then
	rm -rf "${REPORT_DIR}"/*
else
	mkdir -p "${REPORT_DIR}"
fi
echo "" > "${REPORT_FILE}"

do_test() {
	local profile=${1}
	local log=${2}
	local -n params=${3}
	local status=Failed
	rm -rf openjpa-xmlstore/jdbc:*

	if [[ "${profile}" == *docker ]]; then
		set -x
		mvn -N -P${profile} "${params[@]}" docker:start
		local retCode=$?
		set +x
		if [[ ${retCode} != 0 ]]; then
			echo "Failed -- ${log} (can't start docker)" |tee -a ${REPORT_FILE}
			return 1
		fi
	fi
	set -x
	mvn clean install -P${profile} "${params[@]}" -Drat.skip &> ${REPORT_DIR}/build_${log}.log
	local retCode=$?
	set +x
	if [[ ${retCode} == 0 ]]; then
		status=Passed
	fi
	if [[ "${profile}" == *docker ]]; then
		set -x
		mvn -N -P${profile} "${params[@]}" docker:stop
		set +x
	fi
	echo "${status} -- ${log}" |tee -a ${REPORT_FILE}
	echo "         ----------------- "
}

test_profile() {
	local mode=${1}
	local profile=${2}
	if [[ -n "${START_WITH}" ]]; then
		if [[ "${profile}" == "${START_WITH}" ]]; then
			START_WITH=""
		else
			echo "Skipping ${profile} ..."
			continue
		fi
	fi
	prof="test-${profile}"
	if [[ "${mode}" == "docker" ]]; then
		prof="${prof}-docker"
	fi
	local versions="${VERSIONS[${profile}]}"
	if [[ -n "${versions}" ]]; then
		for ver in ${versions}; do
			par=(-D${profile}.server.version=${ver})
			do_test ${prof} "${profile}_${ver}" par
		done
	else
		par=()
		do_test ${prof} ${profile} par
	fi
}

cd "${PROJECT_ROOT}"

if [[ "${MATRIX[@]}" =~ 'oracle' ]]; then
	mkdir -p "../jdbc_oradata"
	chmod a+rwx "../jdbc_oradata"

	echo -e "\033[0;31mIMPORTANT!\033[0m It will be impossible to clean-up \033[0;31m'../jdbc_oradata'\033[0m please perform manual deletion with sudo";
fi

for profile in ${MATRIX[@]}; do
	test_profile "${FULL_DB_LIST[${profile}]}" "${profile}"
done
