package items.deployable

import items.QgressItem

interface DeployableItem : QgressItem {
    fun getLevel(): Int
}
