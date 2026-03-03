#!/usr/bin/env bash

# A script for setting up vLLM on a rented device

set -euo pipefail

ENV_NAME="vllm-env"
CONDA_DIR="$HOME/miniconda"

# Install Miniconda if missing
if [ ! -d "$CONDA_DIR" ]; then
  wget -q https://repo.anaconda.com/miniconda/Miniconda3-latest-Linux-x86_64.sh
  bash Miniconda3-latest-Linux-x86_64.sh -b -p "$CONDA_DIR"
fi

source "$CONDA_DIR/etc/profile.d/conda.sh"

if ! conda env list | grep -q "$ENV_NAME"; then
  conda create -y -n "$ENV_NAME" python=3.10
fi

conda activate "$ENV_NAME"

# Detect CUDA
CUDA_VERSION=$(nvidia-smi | grep "CUDA Version" | awk '{print $9}')
CUDA_MAJOR=$(echo $CUDA_VERSION | cut -d. -f1)
CUDA_MINOR=$(echo $CUDA_VERSION | cut -d. -f2)

if [ "$CUDA_MAJOR" -ge 12 ]; then
  TORCH_INDEX="https://download.pytorch.org/whl/cu121"
elif [ "$CUDA_MAJOR" -eq 11 ] && [ "$CUDA_MINOR" -ge 8 ]; then
  TORCH_INDEX="https://download.pytorch.org/whl/cu118"
else
  echo "Unsupported CUDA version: $CUDA_VERSION"
  exit 1
fi

pip install --upgrade pip
pip install torch torchvision torchaudio --index-url $TORCH_INDEX
pip install vllm

echo "Installation complete."