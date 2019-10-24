## Minio S3 mTLS Nginx Demo using the AWS S3 SDK

Please read: [Using mTLS to connect to S3 using Java AWS S3 SDK](https://medium.com/sydseter/using-mtls-to-connect-to-s3-using-java-aws-s3-sdk-95c9c1351b5)

We want to be able to authenticate and authorize s3 client connections over a mTLS connection. To do so we will need to install nginx, minio and run some java tests to verify that the system is working.

Warning: Please do not use this code or certificates in production as the code is  inherently insecure and only is meant for demonstrational purpouses. The reason for this is that the Java client application not is setup to verify that is is in fact talking to the correct host. To do this properly you should authorize the host with a implementation of the [javax.net.ssl.HostnameVerifier](https://www.programcreek.com/java-api-examples/?class=javax.net.ssl.HostnameVerifier&method=verify) and use the HostnameVerifier to initialize the SSLContext.

### Installation

Install MinIO: https://docs.min.io/docs/minio-quickstart-guide.html
Install NginX: https://docs.nginx.com/nginx/admin-guide/installing-nginx/installing-nginx-open-source/

### Setup NginX as a mTLS proxy
see server template: [nginx/servers/minio](./nginx/servers/minio)

The certificates and keys needed for setting up mTLS is available under [certs](./certs)

Please change the following folders in the [nginx/servers/minio](./nginx/servers/minio) so that they point to the certificates in the [certs](./certs) folder.

The following properties in [nginx/servers/minio](./nginx/servers/minio) needs to be changed:

- ssl_certificate
- ssl_certificate_key
- ssl_client_certificate

The [nginx/servers/minio](./nginx/servers/minio) needs to be moved to the nginx/servers folder so that it is loaded when restarting nginx.

After installing and starting MinIO and NginX Please verify that mTLS is working by doing the following:

    curl --verbose -X POST -d '{"someparam1":"somevalue1","someparam2":"somevalue2"}' -H "Content-Type: application/json" -k https://localhost:8092 --cert certs/client.pem --key certs/client.key

After the client and server TLS handshake you should see: `SSL connection using TLSv1.2`

### Setup HashiCorp Vault as a KMS

This is needed if you want to run the examples using SSEC using an external KMS like HashiCorp Vault.

1. Follow this setup guide: https://docs.min.io/docs/minio-kms-quickstart-guide.html

But the Minio KMS Environment variables might not work with the latest version of Minio so do like this:

export MINIO_SSE_VAULT_APPROLE_ID=53b11d5a-2e90-2b31-d592-f12fc164913d
export MINIO_SSE_VAULT_APPROLE_SECRET=537d1314-fade-1e16-c6d5-091e8cb11136
export MINIO_SSE_VAULT_ENDPOINT=http://127.0.0.1:8200
export MINIO_SSE_VAULT_AUTH_TYPE=approle
export MINIO_SSE_VAULT_KEY_NAME=my-minio-key
export MINIO_SSE_AUTO_ENCRYPTION=on

Add your minio public.cert to certs/truststore.jks

    keytool -import -alias minio -file ~/.minio/certs/public.crt -storetype JKS -keystore certs/truststore.jks
    # password: server

2. Install and setup AWS-CLI with Minio: https://docs.min.io/docs/aws-cli-with-minio.html

3. Test that Vault and Minio is correctly setup:

        cd src/test/resources

        aws s3api put-object \
         --no-verify-ssl \
         --endpoint-url https://localhost:9000 \
         --bucket test  --key healthTreatment.json \
         --sse-customer-algorithm AES256 \
         --sse-customer-key MzJieXRlc2xvbmdzZWNyZXRrZXltdXN0cHJvdmlkZWQ= \
         --sse-customer-key-md5 7PpPLAK26ONlVUGOWlusfg== \
         --body ./healthTreatment.json

        aws s3api get-object \
         --no-verify-ssl \
         --endpoint-url https://localhost:9000 \
         --bucket test  --key healthTreatment.json \
         --sse-customer-algorithm AES256 \
         --sse-customer-key MzJieXRlc2xvbmdzZWNyZXRrZXltdXN0cHJvdmlkZWQ= \
         --sse-customer-key-md5 7PpPLAK26ONlVUGOWlusfg== \
         ./healthTreatment1.json

If everything succeed, you should have a ./healthTreatment1.json in the resource directory.

### Setup a bucket called test

Configure and install mc: https://docs.min.io/docs/minio-client-complete-guide

mc config host add <ALIAS> http://127.0.0.1:9000 <YOUR-ACCESS-KEY> <YOUR-SECRET-KEY>

mc mb test

### Run the tests to verify that it is working

run tests:
##### Please use the Accesskey and Secretkey from Minio

    export SECRETKEY=<YOUR-SECRET-KEY>
    export ACCESSKEY=<YOUR-ACCESS-KEY>

    mvn test -Dtest=MinioTest

    # or for the SSE-C encryption example
    mvn test -Dtest=MinioSSECTest

Passing all the tests means you have tested the system end-to-end

You should be able to reach Minio S3 by opening https://localhost:8092, 
but you will need to install the [client.p12](certs/client.p12) certificate 
on your Mac/PC and explicitly tell your Mac/PC to trust the certificate in order to access the Minio browser.
