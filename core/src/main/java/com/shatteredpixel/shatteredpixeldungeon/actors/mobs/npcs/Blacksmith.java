/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2023 Evan Debenham
 *
 * Experienced Pixel Dungeon
 * Copyright (C) 2019-2020 Trashbox Bobylev
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Badges;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.Statistics;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.AscensionChallenge;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.items.BrokenSeal;
import com.shatteredpixel.shatteredpixeldungeon.items.EquipableItem;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.PsycheChest;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.shatteredpixel.shatteredpixeldungeon.items.fishingrods.FishingRod;
import com.shatteredpixel.shatteredpixeldungeon.items.keys.KeyToTruth;
import com.shatteredpixel.shatteredpixeldungeon.items.quest.DarkGold;
import com.shatteredpixel.shatteredpixeldungeon.items.quest.Pickaxe;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfUpgrade;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MeleeWeapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.MissileWeapon;
import com.shatteredpixel.shatteredpixeldungeon.journal.Notes;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.Room;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.standard.BlacksmithRoom;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.BlacksmithSprite;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndBlacksmith;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndQuest;
import com.watabou.noosa.Game;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;
import com.watabou.utils.Random;

import java.util.ArrayList;

public class Blacksmith extends NPC {
	
	{
		spriteClass = BlacksmithSprite.class;

		properties.add(Property.IMMOVABLE);
	}
	
	@Override
	protected boolean act() {
		if (Dungeon.hero.buff(AscensionChallenge.class) != null){
			die(null);
			return true;
		}
		if (Dungeon.level.visited[pos] && !Quest.reforged){
			Notes.add( Notes.Landmark.TROLL );
		}
		return super.act();
	}
	
	@Override
	public boolean interact(Char c) {
		
		sprite.turnTo( pos, c.pos );

		if (c != Dungeon.hero){
			return true;
		}
		if (Dungeon.hero.belongings.getItem(KeyToTruth.class) != null){
			Game.runOnRenderThread(new Callback() {
				@Override
				public void call() {
					GameScene.show(new WndQuest(Blacksmith.this, Messages.get(Blacksmith.class, "key")){
						@Override
						public void hide() {
							super.hide();
							Dungeon.hero.belongings.getItem(KeyToTruth.class).detach(Dungeon.hero.belongings.backpack);
							Blacksmith.this.destroy();

							Blacksmith.this.sprite.die();

							Quest.completed = true;

							Badges.truth();
						}
					});
				}
			});
			return true;
		}

		if (!Quest.given) {
			
			Game.runOnRenderThread(new Callback() {
				@Override
				public void call() {
					GameScene.show( new WndQuest( Blacksmith.this,
							Quest.alternative ? Messages.get(Blacksmith.this, "blood_1") : Messages.get(Blacksmith.this, "gold_1") ) {
						
						@Override
						public void onBackPressed() {
							super.onBackPressed();
							
							Quest.given = true;
							Quest.completed = false;
							Notes.add( Notes.Landmark.TROLL );

							Pickaxe pick = new Pickaxe();
							pick.identify();
							if (pick.doPickUp( Dungeon.hero )) {
								GLog.i( Messages.capitalize(Messages.get(Dungeon.hero, "you_now_have", pick.name()) ));
							} else {
								Dungeon.level.drop( pick, Dungeon.hero.pos ).sprite.drop();
							}
						}
					} );
				}
			});

		} else if (!Quest.completed) {
			if (Quest.alternative) {
				
				Pickaxe pick = Dungeon.hero.belongings.getItem( Pickaxe.class );
				if (pick == null) {
					tell( Messages.get(this, "lost_pick") );
				} else if (!pick.bloodStained) {
					tell( Messages.get(this, "blood_2") );
				} else {
					if (pick.isEquipped( Dungeon.hero )) {
						pick.cursed = false; //so that it can always be removed
						pick.doUnequip( Dungeon.hero, false );
					}
					pick.detach( Dungeon.hero.belongings.backpack );
					tell( Messages.get(this, "completed") );
					
					Quest.completed = true;
					Quest.reforged = false;
					Statistics.questScores[2] = 3000;
				}
				
			} else {
				
				Pickaxe pick = Dungeon.hero.belongings.getItem( Pickaxe.class );
				DarkGold gold = Dungeon.hero.belongings.getItem( DarkGold.class );
				if (pick == null) {
					tell( Messages.get(this, "lost_pick") );
				} else if (gold == null || gold.quantity() < 15) {
					tell( Messages.get(this, "gold_2") );
				} else {
					if (pick.isEquipped( Dungeon.hero )) {
						pick.doUnequip( Dungeon.hero, false );
					}
					pick.detach( Dungeon.hero.belongings.backpack );
					gold.detachAll( Dungeon.hero.belongings.backpack );
					tell( Messages.get(this, "completed") );
					
					Quest.completed = true;
					Quest.reforged = false;
					Statistics.questScores[2] = 3000;
				}
				
			}
		} else if (!Quest.reforged && Dungeon.gold >= getGold()) {
			
			Game.runOnRenderThread(new Callback() {
				@Override
				public void call() {
					GameScene.show( new WndBlacksmith( Blacksmith.this, Dungeon.hero ) );
				}
			});
			
		} else {
			
			tell( Messages.get(this, "get_lost",
					getGold()));
			
		}

		return true;
	}
	
	private void tell( String text ) {
		Game.runOnRenderThread(new Callback() {
			@Override
			public void call() {
				GameScene.show( new WndQuest( Blacksmith.this, text ) );
			}
		});
	}
	
	public static String verify( Item item1, Item item2 ) {
		
		if (item1 == item2 && (item1.quantity() == 1 && item2.quantity() == 1)) {
			return Messages.get(Blacksmith.class, "same_item");
		}

//		if ((item1 instanceof Weapon && !(item2 instanceof Weapon)) ||
//                (item1 instanceof Armor && !(item2 instanceof Armor)) ||
//                (item1 instanceof Wand && !(item2 instanceof Wand)) ||
//                (item1 instanceof Ring && !(item2 instanceof Ring))) {
//			return Messages.get(Blacksmith.class, "diff_type");
//		}
		
		if (!item1.isIdentified() || !item2.isIdentified()) {
			return Messages.get(Blacksmith.class, "un_ided");
		}
		
		if (item1.cursed || item2.cursed ||
				(item1 instanceof Armor && ((Armor) item1).hasCurseGlyph()) ||
				(item2 instanceof Armor && ((Armor) item2).hasCurseGlyph()) ||
				(item1 instanceof Weapon && ((Weapon) item1).hasCurseEnchant()) ||
				(item2 instanceof Weapon && ((Weapon) item2).hasCurseEnchant())) {
			return Messages.get(Blacksmith.class, "cursed");
		}
		
		if (item1.level() < 0 || item2.level() < 0) {
			return Messages.get(Blacksmith.class, "degraded");
		}
		
		if (!item1.isUpgradable() || !item2.isUpgradable()) {
			return Messages.get(Blacksmith.class, "cant_reforge");
		}
		
		return null;
	}
	
	public static void upgrade( Item item1, Item item2 ) {
		
		Item first, second;
        first = item1;
        second = item2;
		if (item1 instanceof MeleeWeapon && item2 instanceof MeleeWeapon){
		    if ((((MeleeWeapon) item2).tier > ((MeleeWeapon) item1).tier)){
                first = item2;
                second = item1;
            }
        } else if (item1 instanceof MissileWeapon && item2 instanceof MissileWeapon){
            if ((((MissileWeapon) item2).tier > ((MissileWeapon) item1).tier)){
                first = item2;
                second = item1;
            }
        } else if (item1 instanceof Armor && item2 instanceof Armor){
            if ((((Armor) item2).tier > ((Armor) item1).tier)){
                first = item2;
                second = item1;
            }
        }
		if (second.level() > first.level() || first instanceof FishingRod) {
		    Item temp = first;
			first = second;
			second = temp;
		}

		Sample.INSTANCE.play( Assets.Sounds.EVOKE );
		ScrollOfUpgrade.upgrade( Dungeon.hero );
		Item.evoke( Dungeon.hero );

		if (second.isEquipped( Dungeon.hero )) {
			((EquipableItem)second).doUnequip( Dungeon.hero, false );
		}
		second.detach( Dungeon.hero.belongings.backpack );

		if (second instanceof Armor){
			BrokenSeal seal = ((Armor) second).checkSeal();
			if (seal != null){
				Dungeon.level.drop( seal, Dungeon.hero.pos );
			}
		}

		if (first.isEquipped( Dungeon.hero )) {
			((EquipableItem)first).doUnequip( Dungeon.hero, true );
		}
		if (first instanceof MissileWeapon && first.quantity() > 1){
			first = first.split(1);
		}
		int level = first.level();
		//adjust for curse infusion
		if (first instanceof Weapon && ((Weapon) first).curseInfusionBonus) level--;
		if (first instanceof Armor && ((Armor) first).curseInfusionBonus) level--;
		if (first instanceof Wand && ((Wand) first).curseInfusionBonus) level--;
		first.level(level+second.level()+1); //prevents on-upgrade effects like enchant/glyph removal
		if (first instanceof MissileWeapon && !Dungeon.hero.belongings.contains(first)) {
			if (!first.collect()){
				Dungeon.level.drop( first, Dungeon.hero.pos );
			}
		}
		Dungeon.gold -= getGold();
		Dungeon.hero.spendAndNext( 2f );
		Badges.validateItemLevelAquired( first );
		Item.updateQuickslot();

		Notes.remove( Notes.Landmark.TROLL );
	}

	public static int getGold() {
		return (45 + 15 * Dungeon.escalatingDepth()) * 25;
	}

	@Override
	public int defenseSkill( Char enemy ) {
		return INFINITE_EVASION;
	}
	
	@Override
	public void damage( int dmg, Object src ) {
	}
	
	@Override
	public void add( Buff buff ) {
	}
	
	@Override
	public boolean reset() {
		return true;
	}

	public static class Quest {
		
		private static boolean spawned;
		
		private static boolean alternative;
		private static boolean given;
		private static boolean completed;
		private static boolean reforged;
		
		public static void reset() {
			spawned		= false;
			given		= false;
			completed	= false;
			reforged	= false;
		}
		
		private static final String NODE	= "blacksmith";
		
		private static final String SPAWNED		= "spawned";
		private static final String ALTERNATIVE	= "alternative";
		private static final String GIVEN		= "given";
		private static final String COMPLETED	= "completed";
		private static final String REFORGED	= "reforged";
		
		public static void storeInBundle( Bundle bundle ) {
			
			Bundle node = new Bundle();
			
			node.put( SPAWNED, spawned );
			
			if (spawned) {
				node.put( ALTERNATIVE, alternative );
				node.put( GIVEN, given );
				node.put( COMPLETED, completed );
				node.put( REFORGED, reforged );
			}
			
			bundle.put( NODE, node );
		}
		
		public static void restoreFromBundle( Bundle bundle ) {

			Bundle node = bundle.getBundle( NODE );
			
			if (!node.isNull() && (spawned = node.getBoolean( SPAWNED ))) {
				alternative	=  node.getBoolean( ALTERNATIVE );
				given = node.getBoolean( GIVEN );
				completed = node.getBoolean( COMPLETED );
				reforged = node.getBoolean( REFORGED );
			} else {
				reset();
			}
		}
		
		public static ArrayList<Room> spawn( ArrayList<Room> rooms ) {
			if (!spawned && (Dungeon.depth == PsycheChest.questDepth ||
					(Dungeon.depth > 11 && Random.Int( 15 - Dungeon.depth ) == 0))) {
				
				rooms.add(new BlacksmithRoom());
				spawned = true;
                alternative = Dungeon.cycle == 0 && Random.Int(2) == 0;


				PsycheChest.questDepth = -1;
				given = false;
				
			}
			return rooms;
		}
	}
}
