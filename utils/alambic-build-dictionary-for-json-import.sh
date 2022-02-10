#!/bin/bash
ITEM_COUNT=1
echo "{\"dictionary\":[]}" > $2
while read line
do 
  echo "Treating #${ITEM_COUNT} : ${line} ..."
  EFN=$(echo "${line}" | sed -e 's/\(.*\)/\L\1/' | sed -r 's# #@@#g'); sed -i -r 's#\]\}$#{"element":"'${3}'", "value":"'${EFN}'"},]}#' $2
  ITEM_COUNT=$((${ITEM_COUNT} + 1))
done < $1
sed -i -r 's#@@# #g' $2
sed -i -r 's#,\]\}$#]}#' $2
