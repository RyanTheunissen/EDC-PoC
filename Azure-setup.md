# Azure VM setup

When setting up a VM in azure this can be done for free with a student account.

## Initial Setup

Create a new Azure account with your student email [here](https://azure.microsoft.com/en-us/free/students/).

## Create a new Virtual Machine

1. Go to the Azure portal: [https://portal.azure.com/](https://portal.azure.com/)
2. Click on "Create a resource" and select "Virtual Machine".
3. Select the Subscription `Azure for Students`.
4. Choose a Resource group or create a new one if this is the first time.
5. Give your VM a name.
6. Select a region close to you that is included in the Azure for Student regions.
   
   Not all regions are included in the free tier, so make sure to check the [list of available regions](https://portal.azure.com/#view/Microsoft_Azure_Policy/PolicyMenuBlade/~/Assignments).
   
   When you are on that page, click on `Authoring`, `Assignments`, and then `Allowed resource deployment regions`.

   Under `Parameters`, `Parameter value` you will find a list of regions that are allowed for your subscription.
7. Leave `Availability options`, `Zone options` and `Availability zone` as default.
8. Do the same for `Security type`.
9. Choose an image, for example `Ubuntu Server 24.04 LTS - x64 Gen2`.
10. Choose a size, for example `Standard B1s`. This size is included in the free tier.
11. Set up authentication, for example with SSH public key. Also set the inbound port rules to allow SSH (port 22). This is already selected by default when you select SSH as authentication type.
12. Make sure to copy your SSH public key to a safe place, you will need it to connect to the VM.
13. Leave the rest of the settings as default and create the VM.
14. After the VM is created, go to the VM overview page and copy the public IP address. You will need this to connect to the VM.