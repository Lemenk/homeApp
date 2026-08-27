export interface MemberVO {
  userId: number
  nickname: string
  avatar?: string
  role: string
  joinedAt: string
}

export interface FamilyVO {
  id: number
  name: string
  creatorId: number
  inviteCode: string
  role: string
  members: MemberVO[]
}
