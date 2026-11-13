# AxialClient PayNow product mappings

Store: `559186693647761408`. Fulfilment reads `paynow.json` on each webhook
request. Product IDs are strings; preserve all digits when copying them.

| PayNow product ID | Product | Cosmetic ID |
| --- | --- | --- |
| `619762575072043008` | Staff | `staff` |
| `624788824383295488` | White Sneakers | `white_sneakers` |
| `624789316765229056` | Swim Bands | `swim_bands` |
| `624790151096176640` | Purrr Cape | `purrr` |
| `624789572366114816` | Ribcage | `ribcage` |
| `624807205576642560` | Ducking Around | `ducking_around` |
| `624815138351882240` | Magenta City | `magenta_city` |

Use one-time purchases and the `axial-client-cosmetics` tag slug for the website.
The existing signed order/refund/chargeback webhook integration handles grants;
no game-server commands are needed. The product mapping is independent of the
website tag: a product must have both configured to be listed and fulfilled.

The website also maps these explicit IDs to their corresponding mannequin/model
previews, so renaming a product does not select an incorrect preview. Rebuild and
upload its `dist` folder when changing website preview mappings.

`tests/paynow.php` tests each new product's completed purchase and full refund
with synthetic events, test-only signing secrets and an in-memory database.
Complete a real zero-cost/100%-coupon checkout to verify PayNow webhook delivery
and appearance in the recipient's wardrobe before selling.
