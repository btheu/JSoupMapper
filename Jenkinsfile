#! /bin/groovy
import PipelineTools
def tools = new PipelineTools()

node('slave-build') {

  tools.runPipeline({

    def rules = tools.defaultRules()

    properties([
      buildDiscarder(logRotator(numToKeepStr: '50'))
    ])

    def rule = tools.findRule(env.BRANCH_NAME,rules)
    def context = [:]

    tools.checkoutBase(rule,context)

    tools.compileBase(rule,context)

    tools.deployToNexusBase(rule,context)

    tools.sonarBase(rule,context)
  })
}