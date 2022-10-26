package io.hyperfoil.tools.parse.factory;

import io.hyperfoil.tools.parse.Parser;
import io.hyperfoil.tools.yaup.json.Json;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.*;

public class JenkinsPipelineFactoryTest {

    private static JenkinsPipelineFactory f;

    @BeforeClass
    public static void staticInit(){
        f = new JenkinsPipelineFactory();
    }

    @Test
    public void node_with_name(){
        Parser p = f.newParser();
        p.setState("pipeline",true);
        p.onLine("node('foo')");
        Json result = p.close();
    }

    @Test
    public void script_multiline(){
        Parser p = f.newParser();
        p.setState("pipeline",true);
        p.onLine("script {");
        p.onLine("def r = sh script: 'oc get crd hyperfoils.hyperfoil.io', returnStatus: true  ");
        p.onLine("return (r == 0);");
        Json result = p.close();
    }
    @Test
    public void script_multiline_closes(){
        Parser p = f.newParser();
        p.setState("pipeline",true);
        p.onLine("                script {");
        p.onLine("                    args = params.entrySet().collect{e -> \"-S \"+e.getKey()+\"=\"+e.getValue()}.join(\" \")                    ");
        p.onLine("                }");
        Json result = p.close();
        assertEquals(0,p.getCount("script"));
        assertFalse("script should be disabled",p.getState("script"));
    }
    @Test @Ignore
    public void script_multiline_multiple_close_lines(){
        Parser p = f.newParser();
        p.setState("pipeline",true);
        p.onLine("script {");
        p.onLine("  [ 'mwperf-server10', 'mwperf-server11', 'mwperf-server12', 'mwperf-server13', 'mwperf-server14', 'mwperf-server15','mwperf-server16' ].each {");
        p.onLine("    def STATUS=sh(returnStdout: true, script: 'ipmitool -I lanplus -U ${MWPERF_IPMI_USR} -P ${MWPERF_IPMI_PSW} ' + \"-H ${it}-drac.mgmt.perf.lab.eng.rdu2.redhat.com power status\")");
        p.onLine("    if (!STATUS.trim().endsWith(\"off\")) {");
        p.onLine("      sh 'ipmitool -I lanplus -U ${MWPERF_IPMI_USR} -P ${MWPERF_IPMI_PSW} ' + \"-H ${it}-drac.mgmt.perf.lab.eng.rdu2.redhat.com power off\"");
        p.onLine("    }");
        p.onLine("  }");
        p.onLine("}");
        Json result = p.close();
        assertEquals("script",result.get("section"));
        assertTrue("value should be json\n"+result.get("value"),result.get("value") instanceof Json);
        Json value = result.getJson("value");
        assertTrue("value should be an array\n"+value,value.isArray());
        assertEquals("value should have 6 entries\n"+value,6,value.size());
        assertEquals(0,p.getCount("script"));
        assertFalse("script should be disabled",p.getState("script"));

    }
    @Test @Ignore
    public void script_inline_complete(){
        Parser p = f.newParser();
        p.setState("pipeline",true);
        String arg = "args = params.entrySet().collect{e -> \"-S \"+e.getKey()+\"=\"+e.getValue()}.join(\" \")";
        p.onLine("script { "+arg+"}  ");
        Json result = p.close();
        assertEquals("script",result.get("section"));
        assertTrue("value should be json\n"+result.get("value"),result.get("value") instanceof Json);
        Json value = result.getJson("value");
        assertTrue("value should be an array\n"+value,value.isArray());
        assertEquals("value",arg,value.get(0));
        assertEquals(0,p.getCount("script"));
        assertFalse("script should be disabled",p.getState("script"));
    }

    @Test
    public void timeout_int(){
        Parser p = f.newParser();
        p.setState("pipeline",true);
        p.onLine("timeout(180) {");
        Json result = p.close();
    }

    @Test  @Ignore
    public void plus_same_line(){
        Parser p = f.newParser();
        p.setState("pipeline",true);
        p.onLine("sh 'foo' + \" bar\"");
        Json result = p.close();

        assertEquals("name","sh",result.get("name"));
        assertEquals("value","foo bar",result.get("value"));
    }
    @Test
    public void plus_more_string(){
        Parser p = f.newParser();
        p.setState("pipeline",true);
        p.onLine("value 'foo' + ");
        p.onLine("'bar' +");
        p.onLine("'biz'");
        p.onLine("'buz'");
        Json result = p.close();
        assertEquals("plus should append string","foobarbiz",result.get("value"));
    }

    @Test @Ignore
    public void step_key_value_arg(){
        Parser p = f.newParser();
        p.setState("pipeline",true);
        p.onLine("archiveArtifacts artifacts: 'run/**/*.*', fingerprint: false");
        Json result = p.close();
        assertEquals("archiveArtifacts",result.get("name"));
        assertEquals("run/**/*.*",result.get("artifacts"));
        assertEquals(false,result.get("fingerprint"));
    }
    @Test @Ignore
    public void step_quoted_arg(){
        Parser p = f.newParser();
        p.setState("pipeline",true);
        p.onLine("sh 'something with spaces'");
        Json result = p.close();
        assertEquals("name","sh",result.get("name"));
        assertEquals("value","something with spaces",result.get("value"));
    }
    @Test @Ignore
    public void step_quoted_arg_plus_more(){
        Parser p = f.newParser();
        p.setState("pipeline",true);
        p.onLine("sh 'something with spaces ' +");
        p.onLine("\"and extra lines\"");
        Json result = p.close();
        assertEquals("name","sh",result.get("name"));
        assertEquals("value","something with spaces and extra lines",result.get("value"));
    }



    @Test @Ignore
    public void node(){
        Parser p = f.newParser();
        p.setState("pipeline",true);
        Json result;
        p.onLine("  node {");
        result = p.close();
        assertEquals("should be array with empty object",Json.fromString("{node:[{}]}"),result);


        p.onLine("  node('foo')");
        result = p.close();


        p.onLine("  node('foo') {");
        result = p.close();

    }

    @Test @Ignore
    public void pipeline_agent_node(){
        Parser p = f.newParser();
        Arrays.asList(
              "pipeline {",
              "  agent {",
              "    node('foo')",
              "  }",
              "}"
        ).stream().forEach(p::onLine);
        Json root = p.getBuilder().getRoot();
    }
    @Test @Ignore
    public void pipeline_agent_environment(){
        Parser p = f.newParser();
        Arrays.asList(
                "pipeline {",
                "  agent {",
                "    node('foo')",
                "  }",
                "  environment {",
                "    GITLAB = credentials('perf-ci')",
                "  }",
                "}"
        ).stream().forEach(p::onLine);
        Json root = p.getBuilder().getRoot();
    }

    @Test @Ignore
    public void pipeline_stage_step(){
        Parser p = f.newParser();
        Arrays.asList(
                "pipeline {",
                "    stages {",
                "        stage('checkout') {",
                "            steps {",
                "              sh \"ls\"",
                "            }",
                "        }",
                "    }",
                "}"

        ).stream().forEach(p::onLine);
        Json root = p.getBuilder().getRoot();
    }

    @Test @Ignore
    public void pipeline_parameter_singleine(){
        Parser p = f.newParser();
        Arrays.asList(
                "pipeline {",
                "    parameters {",
                "        string(name: 'DEPLOY_ENV', defaultValue: 'staging', description: '')",
                "    }",
                "}"

        ).stream().forEach(p::onLine);
        Json root = p.getBuilder().getRoot();
    }
    @Test @Ignore
    public void pipeline_parameters(){
        Parser p = f.newParser();
        Arrays.asList(
                "pipeline {",
                "    parameters {",
                "        string(",
                "            name: 'namespace_count',",
                "            defaultValue: '5000',",
                "            description: 'number of namespaces to create',",
                "            trim: true",
                "        )",
                "        string(name: 'DEPLOY_ENV', defaultValue: 'staging', description: '')",
                "        text(name: 'DEPLOY_TEXT', defaultValue: 'One\\nTwo\\nThree\\n', description: '')",
                "        booleanParam(name: 'DEBUG_BUILD', defaultValue: true, description: '')",
                "        choice(name: 'CHOICES', choices: ['one', 'two', 'three'], description: '')",
                "        password(name: 'PASSWORD', defaultValue: 'SECRET', description: 'A secret password')",
                "        string(",
                "            name: 'service_count',",
                "            defaultValue: '1',",
                "            description: 'number of services to create in each namespace',",
                "            trim: true",
                "        )",
                "    }",
                "}"

        ).stream().forEach(p::onLine);
        Json root = p.getBuilder().getRoot();
    }
    @Test
    public void pipeline_template(){
        Parser p = f.newParser();
        Arrays.asList(
            "pipeline {",
            "    agent {",
            "        node('mwperf-ocp')",
            "    }",
            "    environment {",
            "        GITLAB = credentials('perf-ci')",
            "    }",
            "    options {",
            "        lock('mwperf-ocp')",
            "    }",
            "    parameters {",
            "        string(",
            "            name: 'namespace_count',",
            "            defaultValue: '5000',",
            "            description: 'number of namespaces to create',",
            "            trim: true",
            "        )",
            "        string(",
            "            name: 'service_count',",
            "            defaultValue: '1',",
            "            description: 'number of services to create in each namespace',",
            "            trim: true",
            "        )",
            "        string(",
            "            name: 'check_service_condition',",
            "            defaultValue: 'true',",
            "            description: 'check if serverless service is available',",
            "            trim: true",
            "        )",
            "",
            "        string( ",
            "            name: 'COMMIT',",
            "            defaultValue: 'wreicher-serverless-namespaces',",
            "            description: '',",
            "            trim: true",
            "        )",
            "    }",
            "    stages {",
            "        stage('checkout') {",
            "            steps {",
            "                checkout(",
            "                    [$class: 'GitSCM'",
            "                    , branches: [[name: \"${COMMIT}\"]], ",
            "                    , doGenerateSubmoduleConfigurations: true",
            "                    , extensions: [[$class: 'SubmoduleOption', parentCredentials: true]]",
            "                    , submoduleCfg: []",
            "                    , userRemoteConfigs: [[url: \"${SCRIPT_REPO}\" , credentialsId: 'perf-ci']]",
            "                    ]",
            "                )                ",
            "            }",
            "        }",
            "        stage('run') {",
            "            // options {",
            "            //     timeout(time: 180, unit: 'MINUTES')",
            "            // }",
            "            steps {",
            "                script {",
            "                    args = params.entrySet().collect{e -> \"-S \"+e.getKey()+\"=\"+e.getValue()}.join(\" \")                    ",
            "                }",
//            "                withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId:'gitlab', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD']]) {",
//            "                    catchError(buildResult: 'FAILURE', stageResult: 'FAILURE') {",
//            "                        sh \"rm -rf ${WORKSPACE}/run\"",
//            "                        sh \"${JAVA} -version\"",
//            "                        sh \"${JAVA} \" +    ",
//            "                        \"-jar /home/jenkins/jar/qDup-0.6.7-uber.jar \" +",
//            "                        \"-C \"+",
//            "                        \"-B ${WORKSPACE}/run \" +",
//            "                        \"${args} \" +",
//            "                        \"-S _oauth2_token=${PASSWORD} \"+",
//            "                        \"-S oc_username=${USERNAME} \" +",
//            "                        \"-S _oc_password=${PASSWORD} \" +",
//            "                        \"-S JENKINS_ID=${BUILD_NUMBER} \" +",
//            "                        \"${WORKSPACE}/qdup.yaml \"+",
//            "                        \"${WORKSPACE}/core-scripts/ \"",
//            "                    }",
//            "                }",
            "            }",
            "        }",
            "    }",
            "    post {",
            "        always {",
//            "            catchError(buildResult: 'SUCCESS', stageResult: 'FAILURE') {",
//            "                sh \"${JAVA} \" +",
//            "                \"-jar ${PARSE_JAR} \" +",
//            "                \"-r ${WORKSPACE}/parser.yaml \" +",
//            "                \"-s ${WORKSPACE} \"+",
//            "                \"-d ${WORKSPACE}/run/parse.json\"",
//            "",
//            "            }",
            "                horreumUpload (",
            "                    test: \"serverless-create-namespace\"",
            "                    , owner: 'perf-team'",
            "                    , schema: 'urn:serverless-create-namespace:0.1'",
            "                    , access: 'PUBLIC'",
            "                    , start: '$.qdup.timestamps.start'",
            "                    , stop: '$.qdup.timestamps.stop'",
            "                    , jsonFile: \"${WORKSPACE}/run/parse.json\"",
            "                    , quiet: false",
            "                )",
            "",
            "            archiveArtifacts artifacts: 'run/**/*.*', fingerprint: false",
            "        }",
            "    }",
            "}"
        ).stream().forEach(p::onLine);
        Json root = p.getBuilder().getRoot();
    }

}
