def call(body) {
	// evaluate the body block, and collect configuration into the object
	def args= [:]
	body.resolveStrategy = Closure.DELEGATE_FIRST
	body.delegate = args
	body()

	pipeline {
		agent {
			label 'vhost'
		}

		options {
			disableConcurrentBuilds()
			disableResume()
		}


		environment {
			PHP = "${env.PHP_ROOT}${args.PHP_VERSION}\\php.exe"
			VHOST = "${env.VHOST_ROOT}\\${args.VHOST_NAME}"
		}

		stages {
			stage('Install dependencies') {
				steps {
					callShell "$PHP composer.phar update --no-interaction"
				}
			}
			stage('Run PHPUnit') {
				steps {
					callShell "$PHP vendor/phpunit/phpunit/phpunit --log-junit phpunit.results.xml"
					junit 'phpunit.results.xml'
				}
			}
			stage('Deploy to vhost') {
				when {
					branch 'main'
				}
				steps {
					dir("$VHOST") {
						checkout scm
						callShell "git checkout --force -B $BRANCH_NAME origin/$BRANCH_NAME"
						callShell "$PHP composer.phar update --no-interaction --no-dev"
					}
				}
			}
		}
	}
}