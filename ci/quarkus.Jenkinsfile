@Library('hibernate-jenkins-pipeline-helpers@1.5') _

// Avoid running the pipeline on branch indexing
if (currentBuild.getBuildCauses().toString().contains('BranchIndexingCause')) {
  	print "INFO: Build skipped due to trigger being Branch Indexing"
	currentBuild.result = 'NOT_BUILT'
  	return
}

pipeline {
    agent {
        label 'JenkinsDebugging'
    }
    tools {
        jdk 'OpenJDK 17 Latest'
    }
    options {
        buildDiscarder(logRotator(numToKeepStr: '3', artifactNumToKeepStr: '3'))
        disableConcurrentBuilds(abortPrevious: true)
    }
    stages {
        stage('Build') {
        	steps {
				script {
					sh './gradlew publishToMavenLocal -PmavenMirror=nexus-load-balancer-c4cf05fd92f43ef8.elb.us-east-1.amazonaws.com --no-daemon'
					script {
						env.HIBERNATE_VERSION = sh (
								script: "grep hibernateVersion gradle/version.properties|cut -d'=' -f2",
								returnStdout: true
						).trim()
					}
					dir('.release/quarkus') {
// 						sh 'git clone -b orm-update --single-branch https://github.com/beikov/quarkus.git . || git reset --hard && git pull'
// 						sh 'git clone -b main --single-branch https://github.com/quarkusio/quarkus.git . || git reset --hard && git pull'
						sh 'git clone -b 3.5 --single-branch https://github.com/quarkusio/quarkus.git . || git reset --hard && git pull'
						sh "sed -i 's@<hibernate-orm.version>.*</hibernate-orm.version>@<hibernate-orm.version>${env.HIBERNATE_VERSION}</hibernate-orm.version>@' bom/application/pom.xml"
						// Need to override the default maven configuration this way, because there is no other way to do it
						sh "sed -i 's/-Xmx5g/-Xmx2g/' ./.mvn/jvm.config"
						sh "echo -e '\\n-XX:MaxMetaspaceSize=1g'>>./.mvn/jvm.config"
						sh './mvnw -Dquickly install'
						def excludes = "'!integration-tests/kafka-oauth-keycloak,!integration-tests/mongodb-client,!integration-tests/mongodb-panache,!integration-tests/mongodb-panache-kotlin,!integration-tests/mongodb-devservices,!integration-tests/mongodb-rest-data-panache,!integration-tests/liquibase-mongodb,!extensions/mongodb-client/deployment,!extensions/liquibase-mongodb/deployment,!extensions/panache/mongodb-panache/deployment,!extensions/panache/mongodb-panache-kotlin/deployment,!extensions/panache/mongodb-panache-kotlin/runtime,!extensions/panache/mongodb-rest-data-panache/deployment,!extensions/panache/mongodb-panache-common/deployment'"
						sh "./mvnw -pl :quarkus-hibernate-orm -amd -pl ${excludes} verify -Dstart-containers -Dtest-containers"
					}
				}
			}
		}
    }
    post {
        always {
    		configFileProvider([configFile(fileId: 'job-configuration.yaml', variable: 'JOB_CONFIGURATION_FILE')]) {
            	notifyBuildResult maintainers: (String) readYaml(file: env.JOB_CONFIGURATION_FILE).notification?.email?.recipients
            }
        }
    }
}