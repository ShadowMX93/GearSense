# Commands and permissions

Aliases: `/gsense` and `/gs`.

| Command | Permission | Purpose |
| --- | --- | --- |
| `/gearsense on` | `gearsense.use` | Enable tool selection. |
| `/gearsense off` | `gearsense.use` | Disable tool selection. |
| `/gearsense status` | `gearsense.use` | Show personal settings. |
| `/gearsense refill` | `gearsense.refill` | Toggle stack refill. |
| `/gearsense armor` | `gearsense.armor` | Toggle broken-armor replacement. |
| `/gearsense restore` | `gearsense.use` | Toggle original-slot restoration. |
| `/gearsense lock` | `gearsense.use` | Lock or unlock the selected slot. |
| `/gearsense prefer <mode>` | `gearsense.use` | Select `none`, `speed`, `fortune`, `silk-touch`, or `durability`. |
| `/gearsense update status` | `gearsense.update` | Show the last update result. |
| `/gearsense update check` | `gearsense.update` | Check GitHub immediately. |
| `/gearsense update download` | `gearsense.update` | Download a newer release for restart. |
| `/gearsense reload` | `gearsense.admin` | Reload `config.yml`. |

`gearsense.use`, `gearsense.refill`, and `gearsense.armor` default to all
players. `gearsense.update` and `gearsense.admin` default to server operators.
