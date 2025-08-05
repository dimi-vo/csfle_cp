# Client Side Field Level Encryption (CSFLE) in a hybrid setup

We will demo the use of CSFLE with a self-managed SR that will be deployed in Docker.

- [CSFLE in CP](#csfle-in-cp)
    - [Disclaimer](#disclaimer)
    - [AWS KMS Setup](#aws-kms-setup)
        - [Create an IAM User (to get credentials)](#create-an-iam-user-to-get-credentials)
        - [Create a KMS Key](#create-a-kms-key)
        - [Test with the AWS CLI](#test-with-the-aws-cli)
    - [Start CP](#start-cp)
    - [Quick Demo with Kafka console commands](#quick-demo-with-kafka-console-commands)
        - [Produce](#produce)
        - [Consume](#consume)
    - [Java Clients](#java-clients)
        - [Produce](#produce-1)
        - [Consume](#consume-1)
    - [Cleanup](#cleanup)

This repository is based on the original work for CC [here](https://github.com/pneff93/csfle).

## Disclaimer

The code and/or instructions here available are **NOT** intended for production usage.
It's only meant to serve as an example or reference and does not replace the need to follow actual and official
documentation of referenced products.

## Prerequisite

There is already a cluster deployed in CC that is using public networking.

## AWS KMS Setup

To set up AWS KMS (Key Management Service) and get your `AWS_ACCESS_KEY_ID` and `AWS_SECRET_ACCESS_KEY`, follow these
steps:

### Create an IAM User (to get credentials)

These credentials allow access to AWS services (including KMS):

1. Log in to the AWS Console: https://console.aws.amazon.com/
2. Go to IAM (Identity and Access Management).
3. Click Users → Add users.
4. Enter username.
5. Click Next: Permissions:
    - Choose "Attach policies directly"
    - Attach policy `AWSKeyManagementServicePowerUser`.

6. Click through to finish and download the .csv file or copy:
    - AWS_ACCESS_KEY_ID
    - AWS_SECRET_ACCESS_KEY

### Create a KMS Key

1. In the AWS Console, go to KMS.
2. Choose Customer managed keys → Create key (Single-Region key is fine).
3. Key type: Choose Symmetric (common for most use cases).
4. Alias: Give the key a name (e.g., `demo`).
5. Define key administrators and usage permissions (add the IAM user or roles here).
6. Click Finish and note the Key ID or ARN.

### Test with the AWS CLI

Set your credentials temporarily:

```shell
export AWS_ACCESS_KEY_ID=<access-key-id>
export AWS_SECRET_ACCESS_KEY=<secret>
export AWS_DEFAULT_REGION=eu-west-1
```

Check access with:

```shell
aws kms list-keys
```

Encrypt a string (again the key-id presented is a random string and should be replaced yours):

```shell
echo "secret" | aws kms encrypt \
  --key-id <the-id-of-the-key-you-created-in-aws> \
  --plaintext fileb:///dev/stdin \
  --output text \
  --query CiphertextBlob | base64 --decode
```

## Start Schema Registry

We will use a local schema registry, i.e., deployed in docker, and we will configure it to connect to our CC.

For simplicity, we do not use private networking, we use a standard cluster on AWS with public connectivity.

To complete the setup you will need

1. the bootstrap URL of your cluster.
2. to create an API Key for Schema Registry. For testing purposes, you can use your user account as principal for the
   key.

Once you have this information proceed to update the `compose.yml` file.

```yml
SCHEMA_REGISTRY_KAFKASTORE_BOOTSTRAP_SERVERS: <the-bootstrap-url-of-your-cluster>
SCHEMA_REGISTRY_KAFKASTORE_SASL_JAAS_CONFIG: "org.apache.kafka.common.security.plain.PlainLoginModule required username='<CC-API-KEY>' password='CC-API-SECRET';"
```

Then start with SR:

```shell
docker compose up -d
```

## Java Clients

Let's first register our AVRO schema for a new topic `demo-topic`.

Replace the `encrypt.kms.key.id` parameter by the one applicable in your case:

```shell
curl --request POST --url 'http://localhost:8081/subjects/demo-topic-value/versions' --header 'Content-Type: application/vnd.schemaregistry.v1+json' \
--data '{
    "schemaType": "AVRO",  
    "schema": " {\r\n  \"name\": \"PersonalData\",\r\n  \"type\": \"record\",\r\n  \"namespace\": \"com.csfleExample\",\r\n  \"fields\": [\r\n     {\r\n          \"name\": \"id\",\r\n          \"type\": \"string\"\r\n        },\r\n        {\r\n          \"name\": \"name\",\r\n          \"type\": \"string\"\r\n        },\r\n        {\r\n          \"name\": \"birthday\",\r\n          \"type\": \"string\"\r\n, \"confluent:tags\": [ \"PII\"]},\r\n        {\r\n      \"name\": \"timestamp\",\r\n      \"type\": [\r\n        \"string\",\r\n        \"null\"\r\n      ]\r\n    }\r\n  ]\r\n}",
    "ruleSet": {
      "domainRules": [
        {
          "name": "encryptPII",
          "kind": "TRANSFORM",
          "type": "ENCRYPT",
          "mode": "WRITEREAD",
          "tags": ["PII"],
          "params": {
            "encrypt.kek.name": "demo-topic-kek",
            "encrypt.kms.key.id": "<the-arn-of-the-key-you-created-in-aws>",
            "encrypt.kms.type": "aws-kms"
          },
          "onFailure": "ERROR,NONE"
        }
      ]
    }
  }'
  ```

We are registering the [schema of our Kafka clients](./KafkaConsumer/src/main/avro/personalData.avsc) but specifying the
encryption rule for the field `birthday` tagged as `PII`.

### Produce

Let's open a new terminal window and export our variables (replace string values as required):

```shell
export AWS_ACCESS_KEY_ID=<access-key-id>
export AWS_SECRET_ACCESS_KEY=<secret>
export AWS_DEFAULT_REGION=eu-west-1
```

Then, you will need to update the [ProducerProperties.kt](KafkaProducer/src/main/kotlin/ProducerProperties.kt) and
the [ConsumerProperties](KafkaConsumer/src/main/kotlin/ConsumerProperties.kt) with the bootstrap URL so that the client
applications can connect to the cluster.

The credentials for the AWS KMS will be retrieved from the env vars.

And then run (make sure to use Java 17):

```shell
cd KafkaProducer
./gradlew build
./gradlew run
```

After some seconds you should see on Control Center the messages with `birthday` encrypted being produced to our topic
`demo-topic`.

### Consume

Again in another terminal in parallel (and replacing variables as before):

```shell
export AWS_ACCESS_KEY_ID=<access-key-id>
export AWS_SECRET_ACCESS_KEY=<secret>
export AWS_DEFAULT_REGION=eu-west-1
```

We execute the consumer:

```shell
cd KafkaConsumer
./gradlew build
./gradlew run
```

You should be able to consume the messages with the field decrypted.

## Cleanup

```shell
docker compose down -v
```
