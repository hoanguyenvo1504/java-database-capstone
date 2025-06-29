#!/bin/bash

# Simple Interest Calculator

echo "Enter Principal amount:"
read principal

echo "Enter Rate of Interest:"
read rate

echo "Enter Time (in years):"
read time

# Calculate Simple Interest
interest=$(( (principal * rate * time) / 100 ))

echo "Simple Interest is: $interest"

# chmod +x simple-interest.sh
# ./simple-interest.sh
