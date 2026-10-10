package com.mavethedave.herdstead;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.Mob;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HerdStead implements ModInitializer {
	public static final String MOD_ID = "herdstead";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static int tickCounter = 0;

	@Override
	public void onInitialize() {
		HerdsteadConfig.get();
		LOGGER.info("HerdStead initialised.");

		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			ZoneStorage storage = ZoneStorage.get(server);
			ServerLevel overworld = server.getLevel(ServerLevel.OVERWORLD);
			if (overworld != null) {
				storage.migrateZones(overworld);
			}
			LOGGER.info("HerdStead loaded " + storage.zones.size() + " zone(s).");
		});

		ServerTickEvents.END_SERVER_TICK.register(server -> {
			ServerLevel level = server.getLevel(ServerLevel.OVERWORLD);
			if (level == null) {
				return;
			}

			tickCounter++;
			ZoneStorage storage = ZoneStorage.get(server);

			timed("zone growth", () -> storage.stepZoneGrowth(level));

			if (tickCounter % 10 == 0) {
				timed("herd sites", () -> storage.processPendingSites(level));
			}

			if (tickCounter % 20 == 0) {
				timed("depletion", storage::updateDepletionAndRecovery);
				timed("recruitment", () -> storage.attemptRecruitment(level));
				timed("herd AI", () -> HerdManager.updateHerds(level));
			}

			if (tickCounter >= 200) {
				tickCounter = 0;
				timed("reconcile", () -> storage.reconcileLoadedAnimals(level));
				for (ServerPlayer player : server.getPlayerList().getPlayers()) {
					if (player.level() != level) {
						continue;
					}
					timed("zone creation", () -> storage.checkAndCreateZoneForPlayer(player, level));
				}

				// One line per zone: species=current+pending/max, with (dN) when depleted. Debug only.
				if (HerdsteadConfig.get().debugLogging) {
					for (Zone zone : storage.zones) {
						Map<String, Integer> pending = zone.pendingBySpecies();
						StringBuilder line = new StringBuilder(zone.name).append(":");
						for (Map.Entry<String, AnimalPopulation> entry : zone.populations.entrySet()) {
							AnimalPopulation pop = entry.getValue();
							if (pop.maxPopulation <= 0) {
								continue;
							}
							line.append(' ').append(entry.getKey().replace("minecraft:", ""))
									.append('=').append(pop.currentPopulation)
									.append('+').append(pending.getOrDefault(entry.getKey(), 0))
									.append('/').append(pop.maxPopulation);
							if (pop.depletion > 0) {
								line.append("(d").append(pop.depletion).append(')');
							}
						}
						LOGGER.info(line.toString());
					}
				}
			}
		});

		ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
			if (entity instanceof Animal animal && animal.level() instanceof ServerLevel serverLevel) {
				// A wolf (or any other mob) killing it is predation, not over-hunting.
				boolean predatorKill = damageSource.getEntity() instanceof Mob;
				ZoneStorage.get(serverLevel.getServer()).onTrackedAnimalDied(animal, !predatorKill);
			}
		});

		UseEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
			if (level instanceof ServerLevel serverLevel && entity instanceof Animal animal) {
				AnimalSpecies species = AnimalRegistry.getForEntity(animal);
				if (species != null) {
					ItemStack stack = player.getItemInHand(hand);
					if (species.breedingItems.contains(stack.getItem())
							&& !animal.entityTags().contains(species.domesticTag)) {
						ZoneStorage.get(serverLevel.getServer()).onAnimalDomesticated(animal);
						animal.addTag(species.domesticTag);
					}
				}
			}
			return InteractionResult.PASS;
		});
	}

	private static void timed(String label, Runnable task) {
		long start = System.nanoTime();
		task.run();
		long ms = (System.nanoTime() - start) / 1_000_000;
		if (ms >= 15 && HerdsteadConfig.get().debugLogging) {
			LOGGER.warn("HerdStead: " + label + " took " + ms + " ms");
		}
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}