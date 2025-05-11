# CSFLE in CP

- [CSFLE in CP](#csfle-in-cp)
  - [Disclaimer](#disclaimer)
  - [AWS KMS Setup](#aws-kms-setup)
    - [Create an IAM User (to get credentials)](#create-an-iam-user-to-get-credentials)
    - [Create a KMS Key](#create-a-kms-key)
    - [Test with the AWS CLI](#test-with-the-aws-cli)
  - [Start CP](#start-cp)
  - [Quick Demo with Kafka console commands](#quick-demo-with-kafka-console-commands)

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

Encrypt a string (replace the key-id by your Key Id):

```shell
echo "secret" | aws kms encrypt \
  --key-id alias/my-app-key \
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

