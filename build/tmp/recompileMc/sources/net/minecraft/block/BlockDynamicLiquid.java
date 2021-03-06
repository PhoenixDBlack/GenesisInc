package net.minecraft.block;

import java.util.EnumSet;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;

public class BlockDynamicLiquid extends BlockLiquid
{
    int adjacentSourceBlocks;
    private static final String __OBFID = "CL_00000234";

    protected BlockDynamicLiquid(Material materialIn)
    {
        super(materialIn);
    }

    private void placeStaticBlock(World worldIn, BlockPos pos, IBlockState currentState)
    {
        worldIn.setBlockState(pos, getStaticBlock(this.blockMaterial).getDefaultState().withProperty(LEVEL, currentState.getValue(LEVEL)), 2);
    }

    public void updateTick(World worldIn, BlockPos pos, IBlockState state, Random rand)
    {
        int i = ((Integer)state.getValue(LEVEL)).intValue();
        byte b0 = 1;

        if (this.blockMaterial == Material.lava && !worldIn.provider.doesWaterVaporize())
        {
            b0 = 2;
        }

        int j = this.tickRate(worldIn);
        int i1;

        if (i > 0)
        {
            int k = -100;
            this.adjacentSourceBlocks = 0;
            EnumFacing enumfacing;

            for (Iterator iterator = EnumFacing.Plane.HORIZONTAL.iterator(); iterator.hasNext(); k = this.checkAdjacentBlock(worldIn, pos.offset(enumfacing), k))
            {
                enumfacing = (EnumFacing)iterator.next();
            }

            int l = k + b0;

            if (l >= 8 || k < 0)
            {
                l = -1;
            }

            if (this.getLevel(worldIn, pos.up()) >= 0)
            {
                i1 = this.getLevel(worldIn, pos.up());

                if (i1 >= 8)
                {
                    l = i1;
                }
                else
                {
                    l = i1 + 8;
                }
            }

            if (this.adjacentSourceBlocks >= 2 && this.blockMaterial == Material.water)
            {
                IBlockState iblockstate2 = worldIn.getBlockState(pos.down());

                if (iblockstate2.getBlock().getMaterial().isSolid())
                {
                    l = 0;
                }
                else if (iblockstate2.getBlock().getMaterial() == this.blockMaterial && ((Integer)iblockstate2.getValue(LEVEL)).intValue() == 0)
                {
                    l = 0;
                }
            }

            if (this.blockMaterial == Material.lava && i < 8 && l < 8 && l > i && rand.nextInt(4) != 0)
            {
                j *= 4;
            }

            if (l == i)
            {
                this.placeStaticBlock(worldIn, pos, state);
            }
            else
            {
                i = l;

                if (l < 0)
                {
                    worldIn.setBlockToAir(pos);
                }
                else
                {
                    state = state.withProperty(LEVEL, Integer.valueOf(l));
                    worldIn.setBlockState(pos, state, 2);
                    worldIn.scheduleUpdate(pos, this, j);
                    worldIn.notifyNeighborsOfStateChange(pos, this);
                }
            }
        }
        else
        {
            this.placeStaticBlock(worldIn, pos, state);
        }

        IBlockState iblockstate1 = worldIn.getBlockState(pos.down());

        if (this.canFlowInto(worldIn, pos.down(), iblockstate1))
        {
            if (this.blockMaterial == Material.lava && worldIn.getBlockState(pos.down()).getBlock().getMaterial() == Material.water)
            {
                worldIn.setBlockState(pos.down(), Blocks.stone.getDefaultState());
                this.triggerMixEffects(worldIn, pos.down());
                return;
            }

            if (i >= 8)
            {
                this.tryFlowInto(worldIn, pos.down(), iblockstate1, i);
            }
            else
            {
                this.tryFlowInto(worldIn, pos.down(), iblockstate1, i + 8);
            }
        }
        else if (i >= 0 && (i == 0 || this.isBlocked(worldIn, pos.down(), iblockstate1)))
        {
            Set set = this.getPossibleFlowDirections(worldIn, pos);
            i1 = i + b0;

            if (i >= 8)
            {
                i1 = 1;
            }

            if (i1 >= 8)
            {
                return;
            }

            Iterator iterator1 = set.iterator();

            while (iterator1.hasNext())
            {
                EnumFacing enumfacing1 = (EnumFacing)iterator1.next();
                this.tryFlowInto(worldIn, pos.offset(enumfacing1), worldIn.getBlockState(pos.offset(enumfacing1)), i1);
            }
        }
    }

    private void tryFlowInto(World worldIn, BlockPos pos, IBlockState state, int level)
    {
        if (this.canFlowInto(worldIn, pos, state))
        {
            if (state.getBlock() != Blocks.air)
            {
                if (this.blockMaterial == Material.lava)
                {
                    this.triggerMixEffects(worldIn, pos);
                }
                else
                {
                    state.getBlock().dropBlockAsItem(worldIn, pos, state, 0);
                }
            }

            worldIn.setBlockState(pos, this.getDefaultState().withProperty(LEVEL, Integer.valueOf(level)), 3);
        }
    }

    private int func_176374_a(World worldIn, BlockPos pos, int distance, EnumFacing calculateFlowCost)
    {
        int j = 1000;
        Iterator iterator = EnumFacing.Plane.HORIZONTAL.iterator();

        while (iterator.hasNext())
        {
            EnumFacing enumfacing1 = (EnumFacing)iterator.next();

            if (enumfacing1 != calculateFlowCost)
            {
                BlockPos blockpos1 = pos.offset(enumfacing1);
                IBlockState iblockstate = worldIn.getBlockState(blockpos1);

                if (!this.isBlocked(worldIn, blockpos1, iblockstate) && (iblockstate.getBlock().getMaterial() != this.blockMaterial || ((Integer)iblockstate.getValue(LEVEL)).intValue() > 0))
                {
                    if (!this.isBlocked(worldIn, blockpos1.down(), iblockstate))
                    {
                        return distance;
                    }

                    if (distance < 4)
                    {
                        int k = this.func_176374_a(worldIn, blockpos1, distance + 1, enumfacing1.getOpposite());

                        if (k < j)
                        {
                            j = k;
                        }
                    }
                }
            }
        }

        return j;
    }

    /**
     * This method returns a Set of EnumFacing
     */
    private Set getPossibleFlowDirections(World worldIn, BlockPos pos)
    {
        int i = 1000;
        EnumSet enumset = EnumSet.noneOf(EnumFacing.class);
        Iterator iterator = EnumFacing.Plane.HORIZONTAL.iterator();

        while (iterator.hasNext())
        {
            EnumFacing enumfacing = (EnumFacing)iterator.next();
            BlockPos blockpos1 = pos.offset(enumfacing);
            IBlockState iblockstate = worldIn.getBlockState(blockpos1);

            if (!this.isBlocked(worldIn, blockpos1, iblockstate) && (iblockstate.getBlock().getMaterial() != this.blockMaterial || ((Integer)iblockstate.getValue(LEVEL)).intValue() > 0))
            {
                int j;

                if (this.isBlocked(worldIn, blockpos1.down(), worldIn.getBlockState(blockpos1.down())))
                {
                    j = this.func_176374_a(worldIn, blockpos1, 1, enumfacing.getOpposite());
                }
                else
                {
                    j = 0;
                }

                if (j < i)
                {
                    enumset.clear();
                }

                if (j <= i)
                {
                    enumset.add(enumfacing);
                    i = j;
                }
            }
        }

        return enumset;
    }

    private boolean isBlocked(World worldIn, BlockPos pos, IBlockState state)
    {
        Block block = worldIn.getBlockState(pos).getBlock();
        return !(block instanceof BlockDoor) && block != Blocks.standing_sign && block != Blocks.ladder && block != Blocks.reeds ? (block.blockMaterial == Material.portal ? true : block.blockMaterial.blocksMovement()) : true;
    }

    protected int checkAdjacentBlock(World worldIn, BlockPos pos, int currentMinLevel)
    {
        int j = this.getLevel(worldIn, pos);

        if (j < 0)
        {
            return currentMinLevel;
        }
        else
        {
            if (j == 0)
            {
                ++this.adjacentSourceBlocks;
            }

            if (j >= 8)
            {
                j = 0;
            }

            return currentMinLevel >= 0 && j >= currentMinLevel ? currentMinLevel : j;
        }
    }

    private boolean canFlowInto(World worldIn, BlockPos pos, IBlockState state)
    {
        Material material = state.getBlock().getMaterial();
        return material != this.blockMaterial && material != Material.lava && !this.isBlocked(worldIn, pos, state);
    }

    public void onBlockAdded(World worldIn, BlockPos pos, IBlockState state)
    {
        if (!this.checkForMixing(worldIn, pos, state))
        {
            worldIn.scheduleUpdate(pos, this, this.tickRate(worldIn));
        }
    }
}