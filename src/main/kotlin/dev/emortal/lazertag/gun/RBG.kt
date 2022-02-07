package dev.emortal.lazertag.gun

/*object RBG : ProjectileGun("RGB") {

    override val material: Material = Material.TNT
    override val color: TextColor = NamedTextColor.RED

    override val damage = 9999f
    override val ammo = 9999
    override val reloadTime = 35
    override val cooldown = 5
    override val burstInterval = 1
    override val burstAmount = 5

    override val sound = Sound.sound(SoundEvent.ENTITY_BEE_HURT, Sound.Source.PLAYER, 1f, 1f)

    override fun projectileShot(game: LazerTagGame, player: Player): HashMap<Player, Float> {
        val damageMap = HashMap<Player, Float>()

        return damageMap
    }

    override fun tick(game: LazerTagGame, projectile: Entity) {
        projectile.velocity = projectile.velocity.mul(1.02)

        game.showParticle(
            Particle.particle(
                type = ParticleType.LARGE_SMOKE,
                count = 1,
                data = OffsetAndSpeed()
            ),
            projectile.position.asVec()
        )
    }

    override fun collided(shooter: Player, projectile: Entity) {
        shooter.instance!!.showParticle(
            Particle.particle(
                type = ParticleType.EXPLOSION_EMITTER,
                count = 1,
                data = OffsetAndSpeed(),
            ),
            projectile.position.asVec()
        )

        shooter.instance!!.playSound(
            Sound.sound(SoundEvent.ENTITY_GENERIC_EXPLODE, Sound.Source.PLAYER, 1f, 1f),
            projectile.position
        )

        shooter.instance!!.players
            .filter { it.gameMode == GameMode.ADVENTURE }
            .filter { it.getDistanceSquared(projectile) < 5 * 5 }
            .forEach { loopedPlayer ->
                loopedPlayer.velocity =
                    loopedPlayer.position.sub(projectile.position.sub(.0, .5, .0)).asVec().normalize().mul(80.0)

                loopedPlayer.scheduleNextTick {
                    loopedPlayer.damage(
                        DamageType.fromPlayer(shooter),
                        if (loopedPlayer == shooter) 0f else damage
                    )
                }
            }

        projectile.remove()
    }

    override fun createEntity(shooter: Player): Entity {
        val projectile = Entity(EntityType.BEE)
        val velocity = shooter.position.direction().mul(30.0)

        projectile.velocity = velocity

        projectile.setNoGravity(true)
        projectile.setInstance(shooter.instance!!, shooter.eyePosition())

        return projectile
    }

}*/