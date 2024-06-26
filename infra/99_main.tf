terraform {
  required_providers {
    azurerm = {
      source  = "hashicorp/azurerm"
      version = ">= 3.30.0"
    }
    azuread = {
      source  = "hashicorp/azuread"
      version = "2.30.0"
    }
    azapi = {
      source  = "Azure/azapi"
      version = "= 1.3.0"
    }
    github = {
      source  = "integrations/github"
      version = "5.18.3"
    }
  }

  backend "azurerm" {}
}

provider "azurerm" {
  features {}
}

provider "github" {
  owner = "pagopa"
}

provider "azapi" {}

data "azurerm_subscription" "current" {}

data "azurerm_client_config" "current" {}
