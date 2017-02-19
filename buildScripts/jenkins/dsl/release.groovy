def NAME = "connect-backend"
def BRANCH_NAME = "master"
def EMAIL = "_DevReporting@kenshoo.com"
def STAGING_PLACEMENT = "labs_east"
def PROD_PLACEMENT = "prod_east"
def STAGING_LABEL = "staging"
def PROD_LABEL = "prod"
def JOB_NAME = "${NAME}-release"
def DOCKER_URL = "kenshoo-docker.jfrog.io/$NAME"

job(JOB_NAME) {
    label("centos7-test")
    jdk('Sun JDK 8')

    logRotator(-1,10)
    concurrentBuild(false)

    scm {
        git {
            remote {
                url("git@github.com:kenshoo/${NAME}.git")
                credentials('3f510033-65a9-4afd-9851-c7359bd3f9db')
                refspec("+refs/heads/${BRANCH_NAME}:refs/remotes/origin/${BRANCH_NAME}")
            }

            configure { node ->
                node / 'extensions' / 'hudson.plugins.git.extensions.impl.CleanBeforeCheckout' {}
            }

            branch("$BRANCH_NAME")
        }
    }

    configure { project ->
        def properties = project / 'properties'
        properties<< {
            'com.coravy.hudson.plugins.github.GithubProjectProperty'{
                projectUrl "https://github.com/kenshoo/${NAME}/"
            }
        }
    }

    configure { project ->
        project / 'publishers' << 'net.masterthought.jenkins.CucumberReportPublisher' {
            jsonReportDirectory ""
            fileIncludePattern '**/cucumber.json'
            fileExcludePattern ""
            skippedFails false
            pendingFails false
            undefinedFails false
            missingFails false
            ignoreFailedTests false
            parallelTesting false
        }
    }



    wrappers {
        preBuildCleanup()
        timestamps()
        injectPasswords()
        colorizeOutput()
        timeout {
          absolute(10)
        }
        credentialsBinding {
            usernamePassword('DEPLOYER_USER', 'MICROCOSM_TOKEN', 'MICROCOSM_TOKEN')
        }        
    }

    triggers {
      githubPush()
    }

    steps {
        shell("""
            virtualenv venv
            source venv/bin/activate
            
            pip install microcosm-cli --extra-index-url="https://jenkins:\${AWS_ARTIFACTORY_PASS}@artifactory.kenshoo-lab.com/artifactory/api/pypi/PyPI-releaes/simple/" --trusted-host artifactory.kenshoo-lab.com
            micro init --token xxxxxxxx
            
            #micro validate_structure            
        """)

        gradle {
            useWrapper(false)
            tasks('clean build')
            switches("-PbuildNum=$BUILD_NUMBER ")
        }
        
        shell("""            
            docker login -u microcosm -p \$ARTIFACTORY_MICROCOSM_PASSWORD kenshoo-docker.jfrog.io        
            docker build -t  $DOCKER_URL:\$BUILD_NUMBER .
            docker push $DOCKER_URL:\$BUILD_NUMBER            
        """)

        downstreamParameterized {
            trigger('connect-backend-lab-deploy') {
                parameters {
                    predefinedProp('DOCKER_IMAGE_BUILD', "\${BUILD_NUMBER}")
                }
            }
        }

        shell("""
            source venv/bin/activate

            micro version --placement ${STAGING_PLACEMENT} --app ${NAME} --label ${STAGING_LABEL} --version \${BUILD_NUMBER} --yes            
            micro status --placement ${STAGING_PLACEMENT} --app ${NAME} --label ${STAGING_LABEL} --wait_for READY        
            
        """)
        gradle {
            useWrapper(false)
            tasks('smoketest')
            switches("-Dsmoke.protocol=https -Dsmoke.host=${NAME}-${STAGING_LABEL}.kenshoo-lab.com -Dsmoke.port=443")
        }
        
        shell("""
            source venv/bin/activate

            micro version --placement ${PROD_PLACEMENT} --app ${NAME} --label ${PROD_LABEL} --version \${BUILD_NUMBER} --yes            
            micro status --placement ${PROD_PLACEMENT} --app ${NAME} --label ${PROD_LABEL} --wait_for READY        
            
        """)

    }

    publishers {
        archiveJunit('**/test-results/TEST-*.xml, **/test-results/*/TEST-*.xml, **/test-results.xml')
        extendedEmail("${EMAIL}") {
            trigger(triggerName: 'Unstable',
                    sendToDevelopers: true, sendToRequester: true, includeCulprits: true, sendToRecipientList: false)
            trigger(triggerName: 'Failure',
                    sendToDevelopers: true, sendToRequester: true, includeCulprits: true, sendToRecipientList: false)
            trigger(triggerName: 'StatusChanged',
                    sendToDevelopers: true, sendToRequester: true, includeCulprits: true, sendToRecipientList: false)
            configure { node ->
                node / contentType << 'text/html'
            }
        }
    }
}
