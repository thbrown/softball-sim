## GCP Functions

Softball.app uses Google Cloud Functions and Google Compute Instances to invoke this application from the web.

The content of this package could easily be broken out into its own project and contain a dependency on the broader application.

### How it works

The user wishes to run an optimization using his/her game data and optimizer parameters.

The user sends an HTTP POST to the `softball-sim-start` endpoint and includes data and parameters in the request body.

Behind the scenes the job initially starts on a Google Compute Function. From here, one of two scenarios will happen:

1. If the job can completes before the configured timeout on the function, the result of the optimization will be returned by the request.

2. If the job can not complete before the timeout, the job is transferred to a GCP compute instance.

The job saves its progress to a GCP storage bucket at a set interval (every 5 seconds by default).

If the compute instance is interrupted at any point while executing the job, a shutdown script on the interrupted instance will re-start the job on a new compute instance using the most recent result persisted to the GCP bucket.

The user can, at any point during job execution (function or compute), request the most recent result of the ongoing optimization by sending a HTTP POST request to the `softball-sim-query` endpoint with the same id parameter that was supplied to the `softball-sim-start` endpoint. The return payload will contain a status filed indicating the progress of the job.

The user can, at any point during job execution (function or compute), request that an ongoing optimization be paused by sending a HTTP POST to the `softball-sim-pause` endpoint with the same id parameter that was supplied to the `softball-sim-start` endpoint.

The `softball-sim-compute` function is invoked internally by both the `softball-sim-start` function an by the shutdown script of the Google Compute instances used to run the optimizations.

### Deployment

First, do a gradle build, GCP functions does not yet play nicely with proguard, so you'll want to do skip the proguard step of the build (e.g. "./gradlew build -x proguard")

Then, from the project root directory, run these commands to deploy each function:

`gcloud functions deploy softball-sim-compute --entry-point=com.github.thbrown.softballsim.cloud.GcpFunctionsEntryPointCompute --timeout=120 --memory=256 --runtime=java11 --trigger-http --source=build/libs --max-instances=10 --max-instances=10 --set-env-vars HOME_DIRECTORY=/home/softballdotapp,PROJECT=optimum-library-250223,PASSWORD_HASH=<your_pwd_hash>`

`gcloud functions deploy softball-sim-start --entry-point=com.github.thbrown.softballsim.cloud.GcpFunctionsEntryPointStart --timeout=540 --memory=256 --runtime=java11 --trigger-http --source=build/libs --allow-unauthenticated --max-instances=10 --set-env-vars=COMPUTE_FUNCTION_ENDPOINT=https://us-central1-optimum-library-250223.cloudfunctions.net/softball-sim-compute,PASSWORD_HASH=<your_pwd_hash>`

`gcloud functions deploy softball-sim-query --entry-point=com.github.thbrown.softballsim.cloud.GcpFunctionsEntryPointQuery --timeout=20 --memory=256 --runtime=java11 --trigger-http --source=build/libs --allow-unauthenticated --max-instances=10 --set-env-vars=PASSWORD_HASH=<your_pwd_hash>`

`gcloud functions deploy softball-sim-pause --entry-point=com.github.thbrown.softballsim.cloud.GcpFunctionsEntryPointPause --timeout=20 --memory=256 --runtime=java11 --trigger-http --source=build/libs --allow-unauthenticated --max-instances=10 --set-env-vars=PASSWORD_HASH=<your_pwd_hash>`

### Testing

Unlike running the the application from the command line, which uses the hash of the arguments and data to create an identifier for optimization run. The function requires that you supply your own identifier. `zsjdklasaskfjaskfdjs` is id used in the example.

You will need to replace `<your_pwd>` with your own password and `<your_pwd_hash>` with the sha256 of your password. Don't forget the entry in exampleGcpFunctionsParams.json.

Default parameters are set in `./stats/exampleGcpFunctionsParams.json`

To start an optimization:

`curl -X POST "https://us-central1-optimum-library-250223.cloudfunctions.net/softball-sim-start" -H "Content-Type:application/json" --data @./stats/exampleGcpFunctionsParams.json`

To query for job progress:

`curl -X POST "https://us-central1-optimum-library-250223.cloudfunctions.net/softball-sim-pause" -N -H "Content-Type:application/json" --data '{"i":"zsjdklasaskfjaskfdjs","PASSWORD":"<your_pwd>"}'`

To pause a running optimization:

`curl -X POST "https://us-central1-optimum-library-250223.cloudfunctions.net/softball-sim-pause" -N -H "Content-Type:application/json" --data '{"i":"zsjdklasaskfjaskfdjs","PASSWORD":"<your_pwd>"}'`
