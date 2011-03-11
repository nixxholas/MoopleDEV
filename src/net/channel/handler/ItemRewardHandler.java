/*
	This file is part of the OdinMS Maple Story Server
    Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc>
		       Matthias Butz <matze@odinms.de>
		       Jan Christian Meyer <vimes@odinms.de>

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as
    published by the Free Software Foundation version 3 as published by
    the Free Software Foundation. You may not use, modify or distribute
    this program under any other version of the GNU Affero General Public
    License.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package net.channel.handler;

import client.IItem;
import client.MapleClient;
import client.MapleInventoryType;
import constants.ItemConstants;
import java.rmi.RemoteException;
import java.util.List;
import net.AbstractMaplePacketHandler;
import server.MapleInventoryManipulator;
import server.MapleItemInformationProvider;
import server.MapleItemInformationProvider.RewardItem;
import tools.MaplePacketCreator;
import tools.Pair;
import tools.Randomizer;
import tools.data.input.SeekableLittleEndianAccessor;

/**
 * @author Jay Estrella/ Modified by kevintjuh93
 */
public final class ItemRewardHandler extends AbstractMaplePacketHandler {
    public final void handlePacket(SeekableLittleEndianAccessor slea, MapleClient c) {
        byte slot = (byte) slea.readShort();
        int itemId = slea.readInt(); // will load from xml I don't care.
        if (c.getPlayer().getInventory(MapleInventoryType.USE).getItem(slot).getItemId() != itemId || c.getPlayer().getInventory(MapleInventoryType.USE).countById(itemId) < 1) return;
        MapleItemInformationProvider ii = MapleItemInformationProvider.getInstance();
        Pair<Integer, List<RewardItem>> rewards = ii.getItemReward(itemId);
        for (RewardItem reward : rewards.getRight()) {
            if (!MapleInventoryManipulator.checkSpace(c, reward.itemid, reward.quantity, "")) {
                c.announce(MaplePacketCreator.showInventoryFull());
                break;
            }
            if (Randomizer.nextInt(rewards.getLeft()) < reward.prob) {
		if (ItemConstants.getInventoryType(reward.itemid) == MapleInventoryType.EQUIP) {
                    final IItem item = ii.getEquipById(reward.itemid);
                    if (reward.period != -1) {
			item.setExpiration(System.currentTimeMillis() + (reward.period * 60 * 60 * 10));
                    }
                    MapleInventoryManipulator.addFromDrop(c, item, false);
                } else {
                    MapleInventoryManipulator.addById(c, reward.itemid, reward.quantity);
		}
                if (reward.worldmsg != null) {
                    String msg = reward.worldmsg;
                    msg.replaceAll("/name", c.getPlayer().getName());
                    msg.replaceAll("/item", ii.getName(reward.itemid));
                    try {
                        c.getChannelServer().getWorldInterface().broadcastMessage(null, msg.getBytes());
                    } catch (RemoteException ex) {
                        c.getChannelServer().reconnectWorld();
                    }
                }
            }
        }
        c.announce(MaplePacketCreator.enableActions());
    }
}