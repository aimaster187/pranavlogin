# Domain & Subdomain Setup (`pranavsmm.shop`)

## Required DNS Records (Point to Your VPS Public IPv4 / IPv6)
| Record Type | Host | Target | Purpose |
|---|---|---|---|
| `A` | `@` (`pranavsmm.shop`) | `<VPS_PUBLIC_IP>` | Primary domain |
| `A` | `admin` (`admin.pranavsmm.shop`) | `<VPS_PUBLIC_IP>` | Admin API gateway |
| `A` | `api` (`api.pranavsmm.shop`) | `<VPS_PUBLIC_IP>` | Client DEX & Admin REST endpoint |
| `A` | `bot` (`bot.pranavsmm.shop`) | `<VPS_PUBLIC_IP>` | Telegram bot webhook ingress |

## Enabling HTTPS (Let's Encrypt Certbot)
```bash
sudo cp PRANAV/deployment/nginx/pranavsmm.shop.conf /etc/nginx/sites-available/pranavsmm.shop.conf
sudo ln -sf /etc/nginx/sites-available/pranavsmm.shop.conf /etc/nginx/sites-enabled/
sudo nginx -t && sudo systemctl reload nginx
sudo certbot --nginx -d pranavsmm.shop -d admin.pranavsmm.shop -d api.pranavsmm.shop -d bot.pranavsmm.shop
```
