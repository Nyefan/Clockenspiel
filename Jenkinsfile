pipeline {
  agent any
  stages {
    stage('compile') {
      steps {
        sh '''#!/bin/bash
mvn package'''
      }
    }
  }
}