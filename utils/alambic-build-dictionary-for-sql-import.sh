#!/bin/bash
ITEM_COUNT=1
> $2
while read line
do 
  echo "Treating #${ITEM_COUNT} : ${line} ..."
  EFN=$(echo "${line}" | sed -e 's/\(.*\)/\L\1/' | sed -r 's# #@@#g' | sed -r 's#'\''#'\'\''#g')
  echo "INSERT INTO RandomDictionaryEntity (id, elementname, elementvalue) VALUES (${ITEM_COUNT}, '$3', '${EFN}');" >> $2
  ITEM_COUNT=$((${ITEM_COUNT} + 1))
done < $1
sed -i -r 's#@@# #g' $2
