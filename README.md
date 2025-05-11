# CSFLE in CP

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
It's only meant to serve as an example or reference and does not replace the need to follow actual and official documentation of referenced products.

## AWS KMS Setup

To set up AWS KMS (Key Management Service) and get your `AWS_ACCESS_KEY_ID` and `AWS_SECRET_ACCESS_KEY`, follow these steps:

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

Set your credentials temporarily (the strings bellow are random and you will need to replace by the ones you saved before, as well as the AWS region you will be using):

```shell
export AWS_ACCESS_KEY_ID=7IW0BOS9GQETSSNJK70N
export AWS_SECRET_ACCESS_KEY=BZ8XW9NXOO9S8XO7T313O8J5OIIPSEAD+LHDI8EQ
export AWS_DEFAULT_REGION=eu-west-1
```

Check access with:

```shell
aws kms list-keys
```

Encrypt a string (again the key-id presented is a random string and should be replaced yours):

```shell
echo "secret" | aws kms encrypt \
  --key-id k6q5x2v5-4l0qg-gy6x5anf8-xkais4-bczf \
  --plaintext fileb:///dev/stdin \
  --output text \
  --query CiphertextBlob | base64 --decode
```

## Start CP

Run:

```shell
docker compose up -d
```

You can open [Control Center](http://localhost:9021/) after some seconds. Check logs meanwhile:

```shell
docker compose logs -f
```

## Quick Demo with Kafka console commands

### Produce

Run in a new terminal (replace the random ARN string being used by yours):

```shell
kafka-json-schema-console-producer \
  --broker-list localhost:9091 \
  --topic demo \
  --property value.schema='{"type":"object","properties":{"f1":{"type":"string", "confluent:tags": ["PII"]}}}' --property value.rule.set='{ "domainRules": [ { "name": "encryptPII", "type": "ENCRYPT", "tags":["PII"], "params": { "encrypt.kek.name": "demo", "encrypt.kms.type": "aws-kms", "encrypt.kms.key.id": "arn:aws:kms:eu-west-1:188133902807:key/k6q5x2v5-4l0qg-gy6x5anf8-xkais4-bczf" }, "onFailure": "ERROR,NONE"}]}'
```

And try to produce to it:

```json
{"f1":"test1"}
```

You should get an error `The security token included in the request is invalid`.

So now export the variables again (replace strings accordingly):

```shell
export AWS_ACCESS_KEY_ID=7IW0BOS9GQETSSNJK70N
export AWS_SECRET_ACCESS_KEY=BZ8XW9NXOO9S8XO7T313O8J5OIIPSEAD+LHDI8EQ
export AWS_DEFAULT_REGION=eu-west-1
```

And run the producer again and pass the sample message.

You can check the Control Center and should see the message field encrypted for the `demo` topic.

### Consume

Keep the producer running and open a parallel terminal window and execute:

```shell
kafka-json-schema-console-consumer --topic demo --bootstrap-server localhost:9091
```

Produce another message into the topic. It can be the same one as before.

You can see consumer won't be able to decrypt the message field value and will present the value encrypted. Something like this (the same you see in Control Center):

```json
{"f1":"FaYu4XXxB0g2pTG9LiOhWOao/j9Jqe7Th9Fy4ilbN1+W"}
```

Now in another terminal export the AWS variables first (don't forget to replace by your values):

```shell
export AWS_ACCESS_KEY_ID=7IW0BOS9GQETSSNJK70N
export AWS_SECRET_ACCESS_KEY=BZ8XW9NXOO9S8XO7T313O8J5OIIPSEAD+LHDI8EQ
export AWS_DEFAULT_REGION=eu-west-1
```

And run same command, posting a new message in the producer terminal.

You should be able to see the message unencrypted in the terminal with consumer before but decrypted in the new one.

## Java Clients

Let's first register our AVRO schema for a new topic `demo-topic` (as usual replace the `encrypt.kms.key.id` parameter by the one applicable in your case):

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
            "encrypt.kms.key.id": "arn:aws:kms:eu-west-1:188133902807:key/k6q5x2v5-4l0qg-gy6x5anf8-xkais4-bczf",
            "encrypt.kms.type": "aws-kms"
          },
          "onFailure": "ERROR,NONE"
        }
      ]
    }
  }'
  ```

We are registering the [schema of our Kafka clients](./KafkaConsumer/src/main/avro/personalData.avsc) but specifying the encryption rule for the field `birthday` tagged as `PII`.

### Produce

Let's open a new terminal window and export our variables (replace string values as required):

```shell
export AWS_ACCESS_KEY_ID=7IW0BOS9GQETSSNJK70N
export AWS_SECRET_ACCESS_KEY=BZ8XW9NXOO9S8XO7T313O8J5OIIPSEAD+LHDI8EQ
export AWS_DEFAULT_REGION=eu-west-1
```

And then run (make sure to use Java 17):

```shell
cd KafkaProducer
gradle build
gradle run
```

After some seconds you should see on Control Center the messages with `birthday` encrypted being produced to our topic `demo-topic`.

### Consume

Again in another terminal in parallel (and replacing variables as before):

```shell
export AWS_ACCESS_KEY_ID=7IW0BOS9GQETSSNJK70N
export AWS_SECRET_ACCESS_KEY=BZ8XW9NXOO9S8XO7T313O8J5OIIPSEAD+LHDI8EQ
export AWS_DEFAULT_REGION=eu-west-1
```

We execute the consumer:

```shell
cd KafkaConsumer
gradle build
gradle run
```

You should be able to consume the messages with the field decrypted.

## Cleanup

```shell
docker compose down -v
```