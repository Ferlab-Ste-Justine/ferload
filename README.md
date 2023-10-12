# Ferload

    [![Docker Pulls](https://img.shields.io/docker/pulls/ferlabcrsj/ferload)](https://hub.docker.com/r/ferlab/ferload)
    [![Docker Image Size (latest SEMVER)](https://img.shields.io/docker/image-size/ferlabcrsj/ferload?sort=semver)](https://hub.docker.com/r/ferlab/ferload)
    [![Docker Image Version (latest semver)](https://img.shields.io/docker/v/ferlabcrsj/ferload?sort=semver)](https://hub.docker.com/r/ferlab/ferload)

Ferload is an api that allows to control access to files stored in any object store S3-compliant, and generate presigned url if user is granted.
Ferload is compliant with [GA4GH Data Repository Service](https://ga4gh.github.io/data-repository-service-schemas/). For now, only Bearers tokens are supported (not Passport and Visas).


## Technologies

Ferload is developed in Scala3 and is based on [tapir](https://tapir.softwaremill.com/en/latest/) and [cats-effect](https://typelevel.org/cats-effect/).

## Quick start

If you don't have [sbt](https://www.scala-sbt.org) installed already, you can use the provided wrapper script:

```shell
./sbtx -h # shows an usage of a wrapper script
./sbtx compile # build the project
./sbtx test # run the tests
./sbtx run # run the application (Main)
```

For more details check the [sbtx usage](https://github.com/dwijnand/sbt-extras#sbt--h) page.

Otherwise, if sbt is already installed, you can use the standard commands:

```shell
sbt compile # build the project
sbt test # run the tests
sbt run # run the application (Main)
```

## Build

```shell
sbt asembly
docker build -t ferload .
```

## Environment variables

Keyckloak Authentication server information :

- `AUTH_URL` : Keycloak URL
- `AUTH_REALM` : Keycloak Realm
- `AUTH_CLIENT_ID` : Id of the client that contains resource definition and permissions
- `AUTH_CLIENT_SECRET` : Secret of the client that contains resource definition and permissions
- `AUTH_RESOURCES_POLICY_GLOBAL_NAME` : Name of the resource a user should have access to be able to download all files.
  Works only with endpoints that fetch files by urls. Can be empty.

Ferload Client: This section is used to configure ferload clients taht can be installed to download files by requesting ferload endpoints.
- `FERLOAD_CLIENT_METHOD` : 2 possible values : `token`or `password`. Default `token`.
- `FERLOAD_CLIENT_CLIENT_ID` : client id to use to authenticate user (`password` method) or refesh token (`token` method).
- `FERLOAD_CLIENT_TOKEN_LINK` : url to use to fetch new token in case of `token` method.
- `FERLOAD_CLIENT_TOKEN_HELPER` : text to display in ferload client to explain how to get a new token. Used only if `FERLOAD_CLIENT_METHOD` is `token`. 

  AWS S3 information :

- `AWS_ACCESS_KEY` : Access key of the AWS account
- `AWS_SECRET_KEY` : Secret key of the AWS account
- `AWS_BUCKET` : Default bucket to use if objects are fetched by urls. Can be empty.
- `AWS_ENDPOINT`: Endpoint to S3 service. Can be empty.
- `AWS_PATH_ACCESS_STYLE` : Path access style to S3 service (true for minio, false for AWS). Default false.
- `AWS_PRESIGNED_URL_EXPIRATION_IN_SECONDS` : Expiration time of presigned urls. Default 3600.
- `AWS_REGION` : Region of the AWS account. Can be empty.

DRS Information : 
- `DRS_ID` : DRS Server ID.
- `DRS_NAME` : DRS Name.
- `DRS_ORGANIZATION_NAME` : DRS Organization.
- `DRS_ORGANIZATION_URL` : DRS Organization url. 
- `DRS_SELF_HOST` : DRS self host, used in `self_uri` property of `DrsObject`. 
- `DRS_VERSION` : DRS Version. Default 1.3.0
- `DRS_CONTACT_URL` : DRS contact url. Can be empty.
- `DRS_DESCRIPTION` : DRS description. Can be empty.
- `DRS_DOCUMENTATION_URL` : DRS documentation url. Can be empty.
- `DRS_ENVIRONMENT` :  DRS environment. Can be empty.

HTTP Server information :
- `HTTP_HOST` : Address HTTP server should listen to. Default 0.0.0.0 (all interfaces)
- `HTTP_PORT`: Port HTTP server should listen to. Default 9090

Log configuration :
- `LOG_LEVEL` : Log level. Default WARN.

## Endpoints :

- Swagger : /docs
- Status : /status
- Prometheus : /metrics

## Links:

* [tapir documentation](https://tapir.softwaremill.com/en/latest/)
* [tapir github](https://github.com/softwaremill/tapir)
* [sbtx wrapper](https://github.com/dwijnand/sbt-extras#installation)
