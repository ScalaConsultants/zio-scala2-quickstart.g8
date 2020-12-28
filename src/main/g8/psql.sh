#!/bin/bash
echo "Logging into db items by user postgres "
docker exec -it postgres psql -U postgres -d items
