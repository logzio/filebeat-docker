def NAME = "filebeat-docker"
def BRANCH_NAME = "master"
def EMAIL = "_DevMicrocosm@kenshoo.com"
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
            docker login -u microcosm -p \$ARTIFACTORY_MICROCOSM_PASSWORD kenshoo-docker.jfrog.io        
            docker build -t  $DOCKER_URL:latest -t $DOCKER_URL:\$BUILD_NUMBER .
            docker push $DOCKER_URL
        """)

    }

    publishers {
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
