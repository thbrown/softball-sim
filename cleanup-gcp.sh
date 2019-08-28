#!/bin/sh
NAME=$(curl -X GET http://metadata.google.internal/computeMetadata/v1/instance/name -H 'Metadata-Flavor: Google')
ZONE=$(curl -X GET http://metadata.google.internal/computeMetadata/v1/instance/zone -H 'Metadata-Flavor: Google')
DELETE_ON_SHUTDOWN=$(curl http://metadata.google.internal/computeMetadata/v1/instance/attributes/delete-on-shutdown -H "Metadata-Flavor: Google")
if [ DELETE_ON_SHUTDOWN = "true" ] 
  gcloud --quiet compute instances delete $NAME --zone=$ZONE
else
  echo "delete skiped because metadata value DELETE_ON_SHUTDOWN was not set to true
end